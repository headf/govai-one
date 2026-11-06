package com.govai.enterprise.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.govai.enterprise.common.BusinessException;
import com.govai.enterprise.dto.PreCheckResp;
import com.govai.enterprise.entity.*;
import com.govai.enterprise.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 合规预检服务(申报前 AI 预检)
 * 基于申报表单 + 企业档案 + 电子证照 + 材料清单,做一致性/有效性自动检查
 */
@Service
@RequiredArgsConstructor
public class ComplianceService {

    private final EntApplicationRecordMapper recordMapper;
    private final EntApplicationMaterialMapper materialMapper;
    private final EntMatterInfoMapper matterMapper;
    private final EnterpriseInfoMapper enterpriseInfoMapper;
    private final EnterpriseLicenseMapper licenseMapper;
    private final EntComplianceCheckMapper complianceMapper;
    private final EnterpriseCreditGateService creditGateService;

    /**
     * 对指定申报记录执行合规预检,写入 ent_compliance_check 并返回报告
     */
    public PreCheckResp preCheck(Long enterpriseId, Long recordId) {
        EntApplicationRecord record = recordMapper.selectById(recordId);
        if (record == null || !record.getEnterpriseId().equals(enterpriseId)) {
            throw new BusinessException("申报记录不存在或无权操作");
        }
        EntMatterInfo matter = matterMapper.selectById(record.getMatterId());
        EnterpriseInfo enterprise = enterpriseInfoMapper.selectById(enterpriseId);
        Map<String, Object> form = parseForm(record.getFormData());
        // 企业名下有效证照
        List<EnterpriseLicense> licenses = licenseMapper.selectList(
                new LambdaQueryWrapper<EnterpriseLicense>()
                        .eq(EnterpriseLicense::getEnterpriseId, enterpriseId)
                        .eq(EnterpriseLicense::getStatus, 1));
        // 已上传材料名
        List<String> uploadedNames = materialMapper.selectList(
                        new LambdaQueryWrapper<EntApplicationMaterial>()
                                .eq(EntApplicationMaterial::getRecordId, recordId))
                .stream().map(EntApplicationMaterial::getMaterialName).toList();

        List<PreCheckResp.Item> items = new ArrayList<>();
        // 1. 企业名称一致性(表单涉及该字段时才校验)
        items.add(form.containsKey("enterpriseName")
                ? checkSame("企业名称一致性",
                        form.get("enterpriseName"), enterprise == null ? null : enterprise.getEnterpriseName())
                : skipItem("企业名称一致性"));
        // 2. 统一社会信用代码一致性(含 18 位格式)
        items.add(form.containsKey("creditCode")
                ? checkCreditCode(form.get("creditCode"), enterprise)
                : skipItem("统一社会信用代码核验"));
        // 3. 法定代表人一致性(变更/延续等事项表单不涉及法人填写时跳过)
        items.add(form.containsKey("legalPerson")
                ? checkSame("法定代表人一致性",
                        form.get("legalPerson"), enterprise == null ? null : enterprise.getLegalPerson())
                : skipItem("法定代表人一致性"));
        // 4. 电子证照有效性(企业存在在有效期内的营业执照/许可证)
        items.add(checkLicenseValidity(licenses));
        // 5. 必备材料齐全性(需上传材料是否齐全;免证办材料自动核验)
        if (matter != null) {
            items.add(checkMaterials(matter, uploadedNames, licenses));
        }
        // 6. 企业信用(守信激励/失信受限:D级申办补贴扶持类事项被拦截)
        items.add(checkCredit(enterprise, matter));

        // 写入检查表
        complianceMapper.delete(new LambdaQueryWrapper<EntComplianceCheck>()
                .eq(EntComplianceCheck::getRecordId, recordId)); // 重跑则先清旧记录
        for (PreCheckResp.Item item : items) {
            EntComplianceCheck check = new EntComplianceCheck();
            check.setRecordId(recordId);
            check.setCheckItem(item.getCheckItem());
            check.setCheckResult(item.getResult() ? 1 : 0);
            check.setCheckDetail(item.getDetail());
            complianceMapper.insert(check);
        }

        // 组装报告
        PreCheckResp resp = new PreCheckResp();
        resp.setItems(items);
        resp.setPassCount((int) items.stream().filter(PreCheckResp.Item::getResult).count());
        resp.setFailCount(items.size() - resp.getPassCount());
        resp.setPassed(resp.getFailCount() == 0);
        return resp;
    }

    // ============ 检查项 ============

    /**
     * 企业信用检查:信用好走绿色通道(激励),D级申办补贴扶持类事项受限(拦截)。
     * 监管端离线/未建档按通过处理(fail-open,不误伤申报)。
     */
    private PreCheckResp.Item checkCredit(EnterpriseInfo enterprise, EntMatterInfo matter) {
        PreCheckResp.Item item = new PreCheckResp.Item();
        item.setCheckItem("企业信用");
        if (enterprise == null || matter == null) {
            item.setResult(true);
            item.setDetail("未获取企业信用档案,跳过信用核验");
            return item;
        }
        EnterpriseCreditGateService.GateResult gate = creditGateService.evaluate(enterprise.getId(), matter);
        if ("UNKNOWN".equals(gate.level)) {
            item.setResult(true);
            item.setDetail("信用档案暂未获取,不影响申报");
            return item;
        }
        if (gate.restricted) {
            item.setResult(false);
            item.setDetail("信用不良(D级),暂不可申办补贴扶持类事项,请先办理信用修复");
            return item;
        }
        if (gate.greenChannel) {
            item.setResult(true);
            item.setDetail("信用良好(A级)·绿色通道,申报可享快速办理");
            return item;
        }
        item.setResult(true);
        item.setDetail("信用等级 " + gate.level + ",可正常申报");
        return item;
    }

    /** 跳过校验(本事项表单不涉及该字段) */
    private PreCheckResp.Item skipItem(String itemName) {
        PreCheckResp.Item item = new PreCheckResp.Item();
        item.setCheckItem(itemName);
        item.setResult(true);
        item.setDetail("本事项表单不涉及该项,自动跳过");
        return item;
    }

    private PreCheckResp.Item checkSame(String itemName, Object formValue, Object baseValue) {
        PreCheckResp.Item item = new PreCheckResp.Item();
        item.setCheckItem(itemName);
        String fv = formValue == null ? "" : String.valueOf(formValue).trim();
        String bv = baseValue == null ? "" : String.valueOf(baseValue).trim();
        if (StrUtil.isBlank(fv)) {
            item.setResult(false);
            item.setDetail("申报表单未填写该项,请补全");
        } else if (!fv.equals(bv)) {
            item.setResult(false);
            item.setDetail("填写值(" + fv + ")与企业登记信息(" + bv + ")不一致");
        } else {
            item.setResult(true);
            item.setDetail("与企业登记信息一致");
        }
        return item;
    }

    private PreCheckResp.Item checkCreditCode(Object formValue, EnterpriseInfo enterprise) {
        PreCheckResp.Item item = new PreCheckResp.Item();
        item.setCheckItem("统一社会信用代码核验");
        String code = formValue == null ? "" : String.valueOf(formValue).trim();
        if (!code.matches("[0-9A-Z]{18}")) {
            item.setResult(false);
            item.setDetail("统一社会信用代码应为 18 位数字/大写字母");
            return item;
        }
        if (enterprise != null && !code.equals(enterprise.getCreditCode())) {
            item.setResult(false);
            item.setDetail("代码与企业档案(" + enterprise.getCreditCode() + ")不一致");
            return item;
        }
        item.setResult(true);
        item.setDetail("代码格式正确且与企业档案一致");
        return item;
    }

    private PreCheckResp.Item checkLicenseValidity(List<EnterpriseLicense> licenses) {
        PreCheckResp.Item item = new PreCheckResp.Item();
        item.setCheckItem("电子证照有效性");
        if (licenses.isEmpty()) {
            item.setResult(true);
            item.setDetail("无关联电子证照(本项不强制)");
            return item;
        }
        boolean hasValid = false;
        boolean hasExpired = false;
        for (EnterpriseLicense l : licenses) {
            if (l.getExpiryDate() == null || l.getExpiryDate().isAfter(LocalDate.now())) {
                hasValid = true;
            } else {
                hasExpired = true;
            }
        }
        if (hasExpired) {
            item.setResult(false);
            item.setDetail("存在已过期的证照,请及时更新,否则审批可能被退回");
        } else {
            item.setResult(true);
            item.setDetail("企业证照均在有效期内(" + licenses.size() + " 张)");
        }
        return item;
    }

    private PreCheckResp.Item checkMaterials(EntMatterInfo matter, List<String> uploadedNames,
                                             List<EnterpriseLicense> licenses) {
        PreCheckResp.Item item = new PreCheckResp.Item();
        item.setCheckItem("必备材料齐全性");
        List<String> required = JSONUtil.toList(matter.getMaterials(), String.class);
        List<String> missing = new ArrayList<>();
        if (required != null) {
            for (String m : required) {
                // 已上传 或 可免证办(电子证照自动带出) 视为满足
                boolean isUploaded = uploadedNames.stream().anyMatch(n -> n.equals(m));
                boolean isAutofill = isAutoFillByLicense(m, licenses);
                if (!isUploaded && !isAutofill) {
                    missing.add(m);
                }
            }
        }
        if (missing.isEmpty()) {
            item.setResult(true);
            item.setDetail("所需材料已齐全(上传或电子证照自动核验)");
        } else {
            item.setResult(false);
            item.setDetail("以下材料缺失:" + String.join("、", missing));
        }
        return item;
    }

    private boolean isAutoFillByLicense(String materialName, List<EnterpriseLicense> licenses) {
        if (materialName.contains("营业执照")) {
            return licenses.stream().anyMatch(l -> l.getLicenseType().contains("营业执照"));
        }
        if (materialName.contains("许可证")) {
            return licenses.stream().anyMatch(l -> l.getLicenseType().contains("许可"));
        }
        if (materialName.contains("资质")) {
            return licenses.stream().anyMatch(l -> l.getLicenseType().contains("资质"));
        }
        return false;
    }

    private Map<String, Object> parseForm(String formJson) {
        try {
            return JSONUtil.parseObj(formJson);
        } catch (Exception e) {
            throw new BusinessException("表单数据解析失败");
        }
    }
}
