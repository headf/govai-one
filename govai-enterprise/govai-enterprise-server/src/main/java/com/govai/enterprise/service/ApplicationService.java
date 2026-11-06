package com.govai.enterprise.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.govai.enterprise.common.BusinessException;
import com.govai.enterprise.common.LoginUser;
import com.govai.enterprise.common.PageResult;
import com.govai.enterprise.dto.*;
import com.govai.enterprise.entity.*;
import com.govai.enterprise.feign.ApprovalClient;
import com.govai.enterprise.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 企业申报服务(申报主链路)
 * 流程: 草稿建记录 -> 上传材料/免证办带证 -> 合规预检 -> 最终提交 -> 调审批端
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final EntApplicationRecordMapper recordMapper;
    private final EntApplicationMaterialMapper materialMapper;
    private final EntComplianceCheckMapper complianceMapper;
    private final EntMatterInfoMapper matterMapper;
    private final EnterpriseLicenseMapper licenseMapper;
    private final EntMatterCategoryMapper categoryMapper;
    private final EnterpriseInfoMapper enterpriseInfoMapper;
    private final SubsidyPaymentMapper subsidyPaymentMapper;
    private final FileStorageService fileStorage;
    private final ApprovalClient approvalClient;

    /** 演示模式:审批端不可用时是否允许本地模拟提交(默认 true 便于单机演示;正式联调置 false 则直接报错) */
    @org.springframework.beans.factory.annotation.Value("${govai.mock-login:true}")
    private boolean mockLogin;
    private final LicenseService licenseService;
    private final SupervisionReportService supervisionReportService;
    private final EnterpriseCreditGateService creditGateService;
    private final EntMessageService entMessageService;

    /** 记录号前缀 */
    private static final String CODE_PREFIX = "QY";

    /**
     * 表单预填:返回当前企业信息库的基础字段 + 历史申报记录中填过的字段
     * 用途:已有营业执照等证照的企业申报时,自动回填表单,避免每次手填或拍照 OCR;
     *       延续/变更类事项可复用新办时填过的经营场所、面积、经营项目等数据
     */
    public Map<String, String> prefillEnterpriseInfo(LoginUser user) {
        requireEnterprise(user);
        Map<String, String> prefill = new LinkedHashMap<>();
        // 1. 企业信息库基础字段(企业名称/统一社会信用代码/法定代表人/注册地址/联系电话)
        EnterpriseInfo info = enterpriseInfoMapper.selectById(user.getEnterpriseId());
        if (info != null) {
            putIfNotBlank(prefill, "enterpriseName", info.getEnterpriseName());
            putIfNotBlank(prefill, "creditCode", info.getCreditCode());
            putIfNotBlank(prefill, "legalPerson", info.getLegalPerson());
            putIfNotBlank(prefill, "registerAddress", info.getRegisterAddress());
            putIfNotBlank(prefill, "contactPhone", info.getContactPhone());
        }
        // 2. 从历史申报记录提取 form_data 字段(延续/变更事项复用新办时填过的数据,
        //    如经营场所地址、经营面积、经营项目等,避免重复填写)
        List<EntApplicationRecord> historyRecords = recordMapper.selectList(
                new LambdaQueryWrapper<EntApplicationRecord>()
                        .eq(EntApplicationRecord::getEnterpriseId, user.getEnterpriseId())
                        .ne(EntApplicationRecord::getStatus, "DRAFT")
                        .isNotNull(EntApplicationRecord::getFormData)
                        .orderByDesc(EntApplicationRecord::getCreateTime));
        for (EntApplicationRecord r : historyRecords) {
            if (StrUtil.isBlank(r.getFormData())) {
                continue;
            }
            try {
                cn.hutool.json.JSONObject fd = JSONUtil.parseObj(r.getFormData());
                for (String key : fd.keySet()) {
                    String v = fd.getStr(key);
                    if (StrUtil.isNotBlank(v)) {
                        prefill.putIfAbsent(key, v);
                    }
                }
            } catch (Exception ignored) {
                // form_data 解析失败跳过,不影响预填
            }
        }
        return prefill;
    }

    private void putIfNotBlank(Map<String, String> map, String k, String v) {
        if (StrUtil.isNotBlank(v)) {
            map.put(k, v);
        }
    }

    /**
     * 1. 创建/更新申报草稿(写记录表,状态 DRAFT)
     * 传入 recordId 时更新既有草稿表单,否则新建
     */
    @Transactional
    public ApplySubmitResp createDraft(LoginUser user, ApplySubmitReq req) {
        requireEnterprise(user);
        EntApplicationRecord record;
        if (req.getRecordId() != null) {
            record = recordMapper.selectById(req.getRecordId());
            if (record == null || !record.getEnterpriseId().equals(user.getEnterpriseId())) {
                throw new BusinessException(403, "无权操作该草稿");
            }
            // 草稿可任意编辑;补正退回记录可更新表单(重新提交走 submit);其余状态不允许编辑
            if (!"DRAFT".equals(record.getStatus()) && !"REPAIR".equals(record.getStatus())) {
                throw new BusinessException("该申报已提交,无法编辑");
            }
            record.setMatterId(req.getMatterId());
            record.setFormData(JSONUtil.toJsonStr(req.getFormData()));
            recordMapper.updateById(record);
        } else {
            EntMatterInfo matter = matterMapper.selectById(req.getMatterId());
            if (matter == null || (matter.getStatus() != null && matter.getStatus() == 0)) {
                throw new BusinessException("事项不存在或已下线");
            }
            record = new EntApplicationRecord();
            record.setRecordCode(genRecordCode());
            record.setEnterpriseId(user.getEnterpriseId());
            record.setMatterId(req.getMatterId());
            record.setOperatorId(user.getUserId());
            record.setFormData(JSONUtil.toJsonStr(req.getFormData()));
            record.setStatus("DRAFT");
            record.setSubmitTime(LocalDateTime.now());
            recordMapper.insert(record);
        }

        ApplySubmitResp resp = new ApplySubmitResp();
        resp.setRecordId(record.getId());
        resp.setRecordCode(record.getRecordCode());
        resp.setStatus(record.getStatus());
        return resp;
    }

    /**
     * 2. 最终提交(校验材料、预检、调审批端、更新记录状态)
     */
    @Transactional
    public ApplySubmitResp submit(LoginUser user, Long recordId) {
        requireEnterprise(user);
        EntApplicationRecord record = recordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException("申报记录不存在");
        }
        if (!record.getEnterpriseId().equals(user.getEnterpriseId())) {
            throw new BusinessException(403, "无权操作该申报记录");
        }
        // 状态机:DRAFT/REPAIR -> 提交
        EntMatterInfo matter = matterMapper.selectById(record.getMatterId());
        // 企业信用硬拦:D级企业申办补贴扶持类事项时禁止提交(与合规预检同口径,双保险)
        EnterpriseCreditGateService.GateResult gate =
                creditGateService.evaluate(user.getEnterpriseId(), matter);
        if (gate.restricted) {
            throw new BusinessException("信用受限:贵企业信用等级为 D 级,暂不可申办补贴扶持类事项,请先办理信用修复后再申报");
        }
        // 注销/延续/变更(证照类)必须作用于一张现存有效原证:若表单证号缺失/已失效(如历史草稿、克隆旧单),而企业名下还有同类型有效证,
        // 自动改用最新一张有效证继续提交,避免"拿已注销证号提交"死循环;名下已无有效证才拦截引导先办理新办/延续
        String mn = matter.getMatterName();
        String expectType = mapLicenseType(mn);
        if ((mn.contains("注销") || mn.contains("延续") || mn.contains("变更")) && expectType != null) {
            cn.hutool.json.JSONObject form = StrUtil.isBlank(record.getFormData())
                    ? new cn.hutool.json.JSONObject()
                    : JSONUtil.parseObj(record.getFormData());
            Object licenseNoRaw = form.get("licenseNo");
            String licenseNo = licenseNoRaw == null ? null : String.valueOf(licenseNoRaw);
            List<EnterpriseLicense> actives = licenseMapper.selectList(new LambdaQueryWrapper<EnterpriseLicense>()
                    .eq(EnterpriseLicense::getEnterpriseId, user.getEnterpriseId())
                    .eq(EnterpriseLicense::getStatus, 1)
                    .eq(EnterpriseLicense::getLicenseType, expectType)
                    .orderByDesc(EnterpriseLicense::getId));
            boolean hitActive = StrUtil.isNotBlank(licenseNo)
                    && actives.stream().anyMatch(a -> licenseNo.equals(a.getLicenseNo()));
            if (!hitActive) {
                if (actives.isEmpty()) {
                    throw new BusinessException("名下没有可办理该事项的有效" + expectType + "，请先办理新办/延续取得有效证照后再申报");
                }
                EnterpriseLicense active = actives.get(0);
                form.set("licenseNo", active.getLicenseNo());
                if (form.containsKey("originalExpiryDate")) {
                    form.set("originalExpiryDate", active.getExpiryDate() == null ? "" : active.getExpiryDate().toString());
                }
                record.setFormData(form.toString());
                log.warn("申报 {} 原证[{}]缺失或已失效,已自动改用有效证[{}]继续({})",
                        recordId, licenseNo, active.getLicenseNo(), mn);
            }
        }

        // 新办/开办(证照类)若名下已有同类型有效证照:禁止重复发证(否则同类型会堆叠多张有效证,证照空间显得很乱)
        if ((mn.contains("新办") || mn.contains("开办")) && expectType != null) {
            Long dupCount = licenseMapper.selectCount(new LambdaQueryWrapper<EnterpriseLicense>()
                    .eq(EnterpriseLicense::getEnterpriseId, user.getEnterpriseId())
                    .eq(EnterpriseLicense::getLicenseType, expectType)
                    .eq(EnterpriseLicense::getStatus, 1));
            if (dupCount != null && dupCount > 0) {
                throw new BusinessException("贵企业名下已有有效的「" + expectType + "」，无法重复办理；"
                        + "如需更新信息请选择「变更」，到期延续请选择「延续」，证照遗失请选择「补办」");
            }
        }

        // 材料齐全性校验：事项要求的每份材料，要么已上传，要么属于"免证办"（企业已有对应有效电子证照可自动核验）。
        // 说明：规则引擎只校验表单字段、不看材料，所以若这里不拦，"材料没传齐 + 表单合格"会一路直达办结岗。
        checkMaterialsComplete(user, matter, recordId);

        // 材料相符性弱校验：材料名与原始文件名对不上 → 标记存疑（审批端据此不直达办结、转人工）
        markMaterialSuspectIfAny(record, recordId);

        record.setStatus("PENDING");
        record.setSubmitTime(LocalDateTime.now());

        // 调审批端提交(契约接口4)
        EnterpriseInfo enterprise = enterpriseInfo(user.getEnterpriseId());
        ApprovalSubmitReq approvalReq = new ApprovalSubmitReq();
        approvalReq.setTemplateCode(matter.getMatterCode());
        approvalReq.setMatterId(matter.getId());
        approvalReq.setApplicantName(user.getRealName());
        approvalReq.setApplicantId(enterprise.getCreditCode());
        approvalReq.setApplicantType("ENTERPRISE");
        approvalReq.setFormData(JSONUtil.parseObj(record.getFormData()));
        boolean mocked = false;
        try {
            com.govai.enterprise.common.Result<ApprovalSubmitResp> r =
                    approvalClient.submit(approvalReq);
            if (r != null && r.getData() != null && r.getData().getInstanceId() != null) {
                record.setApprovalInstanceId(r.getData().getInstanceId());
                record.setStatus(r.getData().getStatus());
                // Feign 降级(审批端不可用)会带 mock=true:演示模式允许,正式模式直接报错避免"假贯通"
                mocked = Boolean.TRUE.equals(r.getData().getMock());
                if (mocked && !mockLogin) {
                    throw new BusinessException("审批中心暂不可用，申报未能提交，请稍后重试");
                }
            } else {
                // 审批端有响应但未返回实例(典型:模板停用/参数不合法):必须报错透出,
                // 否则会出现"提交成功但审批端无任何数据"的静默失败
                String msg = r == null ? "审批端无响应"
                        : (StrUtil.isNotBlank(r.getMessage()) ? r.getMessage() : "审批端未返回实例编号");
                throw new BusinessException("提交审批失败：" + msg);
            }
        } catch (BusinessException be) {
            throw be; // 业务错误原样抛给用户,不做降级
        } catch (Exception e) {
            // 审批端不可用(连接失败/超时)
            if (!mockLogin) {
                throw new BusinessException("审批中心暂不可用，申报未能提交，请稍后重试");
            }
            // 演示模式:本地模拟审批实例,保证申报主链路可独立演示
            log.warn("审批端不可用,本地模拟审批: {}", e.getMessage());
            record.setApprovalInstanceId(5001L);
            record.setStatus("APPROVING");
            mocked = true;
        }
        recordMapper.updateById(record);
        // 补正重新提交时清空历史退回意见(updateById 忽略 null,需用 wrapper 强制写 NULL)
        if (StrUtil.isNotBlank(record.getRemark())) {
            record.setRemark(null);
            com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<EntApplicationRecord> uw =
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<EntApplicationRecord>()
                            .eq(EntApplicationRecord::getId, record.getId())
                            .set(EntApplicationRecord::getRemark, null);
            recordMapper.update(null, uw);
        }
        // 上报监管端(大屏真实数据源;失败静默不影响申报)
        supervisionReportService.report(record, matter);

        ApplySubmitResp resp = new ApplySubmitResp();
        resp.setRecordId(record.getId());
        resp.setRecordCode(record.getRecordCode());
        resp.setStatus(record.getStatus());
        resp.setApprovalInstanceId(record.getApprovalInstanceId());
        resp.setMocked(mocked);
        return resp;
    }

    /**
     * 3. 上传申报材料
     */
    @Transactional
    public EntApplicationMaterial uploadMaterial(LoginUser user, Long recordId, String materialName,
                                                  org.springframework.web.multipart.MultipartFile file) {
        EntApplicationRecord record = checkOwnRecord(user, recordId);
        validateMaterialFile(file);   // 基础校验：挡住空文件 / 超大文件 / 非图片·PDF 的无效材料
        EntApplicationMaterial material = new EntApplicationMaterial();
        material.setRecordId(recordId);
        material.setMaterialName(materialName);
        material.setFileUrl(fileStorage.store(file, "material"));
        material.setFileType(file.getContentType());
        material.setOriginalName(file.getOriginalFilename());
        material.setOcrStatus(0);
        materialMapper.insert(material);
        return material;
    }

    /**
     * 材料文件基础校验（服务端强制，前端 accept 只是选择器过滤、可被绕过）：
     * 非空、≤10MB、仅允许图片(JPG/PNG)与 PDF。
     * 说明：无法校验"材料内容是否真实/匹配"（那需要 OCR 或人工），此校验只挡明显无效的文件。
     */
    private void validateMaterialFile(org.springframework.web.multipart.MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("材料文件为空，请重新选择后上传");
        }
        long maxBytes = 10L * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new BusinessException("单个材料文件不能超过 10MB，请压缩后重新上传");
        }
        String ct = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        boolean typeOk = ct.startsWith("image/")
                || ct.equals("application/pdf")
                || name.endsWith(".jpg") || name.endsWith(".jpeg")
                || name.endsWith(".png") || name.endsWith(".pdf");
        if (!typeOk) {
            throw new BusinessException("材料仅支持 JPG / PNG / PDF 格式，当前文件格式不支持");
        }
    }

    /**
     * 可复用证照与历史材料(供材料上传环节从已有证件中选择,避免重复上传)
     */
    public ReusableMaterialVO reusableMaterials(LoginUser user) {
        ReusableMaterialVO vo = new ReusableMaterialVO();
        vo.setLicenses(licenseService.listByEnterprise(user.getEnterpriseId()));

        // 历史已上传材料:企业名下所有申报的材料,按材料名去重(保留最新一条)
        List<EntApplicationRecord> records = recordMapper.selectList(
                new LambdaQueryWrapper<EntApplicationRecord>()
                        .eq(EntApplicationRecord::getEnterpriseId, user.getEnterpriseId())
                        .select(EntApplicationRecord::getId));
        List<ReusableMaterialVO.HistoryMaterial> history = new ArrayList<>();
        if (records != null && !records.isEmpty()) {
            List<Long> recordIds = records.stream().map(EntApplicationRecord::getId).collect(Collectors.toList());
            List<EntApplicationMaterial> mats = materialMapper.selectList(
                    new LambdaQueryWrapper<EntApplicationMaterial>()
                            .in(EntApplicationMaterial::getRecordId, recordIds)
                            .orderByDesc(EntApplicationMaterial::getId));
            Map<String, ReusableMaterialVO.HistoryMaterial> dedup = new LinkedHashMap<>();
            for (EntApplicationMaterial m : mats) {
                if (StrUtil.isBlank(m.getFileUrl()) || dedup.containsKey(m.getMaterialName())) {
                    continue;
                }
                ReusableMaterialVO.HistoryMaterial h = new ReusableMaterialVO.HistoryMaterial();
                h.setMaterialId(m.getId());
                h.setMaterialName(m.getMaterialName());
                h.setFileUrl(m.getFileUrl());
                h.setUploadTime(m.getUploadTime());
                dedup.put(m.getMaterialName(), h);
            }
            history.addAll(dedup.values());
        }
        vo.setHistory(history);
        return vo;
    }

    /**
     * 复用已有材料(证照/历史材料):直接关联已有文件,不重新上传
     */
    @Transactional
    public EntApplicationMaterial reuseMaterial(LoginUser user, Long recordId, ReuseMaterialReq req) {
        checkOwnRecord(user, recordId);
        EntApplicationMaterial material = new EntApplicationMaterial();
        material.setRecordId(recordId);
        material.setMaterialName(req.getMaterialName());
        material.setFileUrl(req.getFileUrl());
        material.setFileType(inferContentType(req.getFileUrl()));
        material.setOcrStatus(0);
        materialMapper.insert(material);
        return material;
    }

    private String inferContentType(String fileUrl) {
        if (fileUrl == null) {
            return null;
        }
        String lower = fileUrl.toLowerCase();
        if (lower.endsWith(".pdf")) {
            return "application/pdf";
        }
        if (lower.endsWith(".svg")) {
            return "image/svg+xml";
        }
        return "image/*";
    }

    /**
     * 4. 免证办:按事项材料清单匹配企业电子证照,返回可自动填充的证照列表
     */
    public List<EnterpriseLicense> matchLicenses(LoginUser user, Long matterId) {
        EntMatterInfo matter = matterMapper.selectById(matterId);
        if (matter == null) {
            throw new BusinessException("事项不存在");
        }
        // 事项材料清单
        List<String> materials = JSONUtil.toList(matter.getMaterials(), String.class);
        // 仅有效证,最新签发在前:注销/延续/变更候选中让"最近一张有效证"排在首位,避免把历史旧证当默认操作对象
        List<EnterpriseLicense> all = licenseMapper.selectList(
                new LambdaQueryWrapper<EnterpriseLicense>()
                        .eq(EnterpriseLicense::getEnterpriseId, user.getEnterpriseId())
                        .eq(EnterpriseLicense::getStatus, 1)
                        .orderByDesc(EnterpriseLicense::getId));
        String matterName = matter.getMatterName();
        // 延续/注销/变更类:要针对"某一张旧证"操作(续期/作废/换发),须精确到该事项对应证照类型,
        // 避免把同关键词的其它证(如食品延续误选卫生证)带出来
        if (matterName.contains("延续") || matterName.contains("注销") || matterName.contains("变更")) {
            String target = mapLicenseType(matterName);
            if (target == null) {
                return new ArrayList<>();
            }
            return all.stream()
                    .filter(l -> target.equals(l.getLicenseType()))
                    .collect(Collectors.toList());
        }
        // 其它(新办/开办等):按材料清单关键词宽匹配可自动填充的证照(免证办)
        return all.stream().filter(l -> {
            if (materials == null) {
                return false;
            }
            for (String m : materials) {
                if (m.contains("营业执照") && l.getLicenseType().contains("营业执照")) {
                    return true;
                }
                if (m.contains("许可证") && l.getLicenseType().contains("许可")) {
                    return true;
                }
                if (m.contains("资质") && l.getLicenseType().contains("资质")) {
                    return true;
                }
            }
            return false;
        }).collect(Collectors.toList());
    }

    /**
     * 5. 我的申报列表
     */
    public PageResult<ApplyRecordVO> pageMyRecords(LoginUser user, int page, int size, String keyword) {
        LambdaQueryWrapper<EntApplicationRecord> wrapper = new LambdaQueryWrapper<EntApplicationRecord>()
                .eq(EntApplicationRecord::getEnterpriseId, user.getEnterpriseId())
                .ne(EntApplicationRecord::getStatus, "DRAFT") // 草稿不出现在正式列表,若需草稿箱再放开
                .orderByDesc(EntApplicationRecord::getCreateTime);
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.like(EntApplicationRecord::getRecordCode, keyword);
        }
        Page<EntApplicationRecord> p = recordMapper.selectPage(new Page<>(page, size), wrapper);
        List<ApplyRecordVO> list = p.getRecords().stream()
                .map(r -> ApplyRecordVO.from(r, matterName(r.getMatterId()), matterCode(r.getMatterId())))
                .collect(Collectors.toList());
        return PageResult.of(list, p.getTotal(), page, size);
    }

    /**
     * 5.1 草稿箱分页列表(仅 DRAFT)
     */
    public PageResult<ApplyRecordVO> pageMyDrafts(LoginUser user, int page, int size, String keyword) {
        LambdaQueryWrapper<EntApplicationRecord> wrapper = new LambdaQueryWrapper<EntApplicationRecord>()
                .eq(EntApplicationRecord::getEnterpriseId, user.getEnterpriseId())
                .eq(EntApplicationRecord::getStatus, "DRAFT")
                .orderByDesc(EntApplicationRecord::getCreateTime);
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.like(EntApplicationRecord::getRecordCode, keyword);
        }
        Page<EntApplicationRecord> p = recordMapper.selectPage(new Page<>(page, size), wrapper);
        List<ApplyRecordVO> list = p.getRecords().stream()
                .map(r -> ApplyRecordVO.from(r, matterName(r.getMatterId()), matterCode(r.getMatterId())))
                .collect(Collectors.toList());
        return PageResult.of(list, p.getTotal(), page, size);
    }

    /**
     * 5.2 删除草稿(连同已上传材料)
     */
    @Transactional
    public void deleteDraft(LoginUser user, Long recordId) {
        EntApplicationRecord record = checkOwnRecord(user, recordId);
        if (!"DRAFT".equals(record.getStatus())) {
            throw new BusinessException("仅草稿可删除");
        }
        materialMapper.delete(new LambdaQueryWrapper<EntApplicationMaterial>()
                .eq(EntApplicationMaterial::getRecordId, recordId));
        recordMapper.deleteById(recordId);
    }

    /**
     * 6. 申报详情(含材料清单 + 审批进度)
     */
    public ApplyDetailVO detail(LoginUser user, Long recordId) {
        EntApplicationRecord record = checkOwnRecord(user, recordId);
        ApplyDetailVO vo = new ApplyDetailVO();
        vo.setRecord(ApplyRecordVO.from(record, matterName(record.getMatterId()), matterCode(record.getMatterId())));
        // 材料
        List<EntApplicationMaterial> materials = materialMapper.selectList(
                new LambdaQueryWrapper<EntApplicationMaterial>()
                        .eq(EntApplicationMaterial::getRecordId, recordId));
        vo.setMaterials(materials);
        // 合规预检
        List<EntComplianceCheck> checks = complianceMapper.selectList(
                new LambdaQueryWrapper<EntComplianceCheck>()
                        .eq(EntComplianceCheck::getRecordId, recordId));
        vo.setChecks(checks);
        // 审批进度(若有实例ID,调审批端;失败则 fallback)
        if (record.getApprovalInstanceId() != null) {
            try {
                com.govai.enterprise.common.Result<ApprovalProgressResp> r =
                        approvalClient.progress(record.getApprovalInstanceId());
                if (r != null && r.getData() != null) {
                    vo.setProgress(r.getData());
                }
            } catch (Exception e) {
                log.warn("查询审批进度异常,返回本地模拟: {}", e.getMessage());
                vo.setProgress(mockProgress(record.getApprovalInstanceId(), record.getSubmitTime()));
            }
        }
        // 补贴/减免到账记录(补贴类事项办结后展示给企业)
        vo.setSubsidies(subsidyPaymentMapper.selectList(
                new LambdaQueryWrapper<SubsidyPayment>()
                        .eq(SubsidyPayment::getRecordId, recordId)
                        .orderByDesc(SubsidyPayment::getId)));
        return vo;
    }

    /**
     * 审批结果回调(审批端推送,企业端接收并处理办结事项)
     * 审批通过时:新办类自动出证、变更类自动同步企业信息、注销类自动禁用证照、延续类自动更新有效期
     */
    @Transactional
    public String handleApprovalResult(Long recordId, String status, String remark) {
        EntApplicationRecord record = recordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException("申报记录不存在");
        }
        // 仅处理审批中的记录
        if (!"APPROVING".equals(record.getStatus()) && !"PENDING".equals(record.getStatus())) {
            throw new BusinessException("该申报当前状态不可变更");
        }
        EntMatterInfo matter = matterMapper.selectById(record.getMatterId());
        String issuedNo = null;
        if ("PASSED".equals(status) || "APPROVED".equals(status)) {
            record.setStatus("PASSED");
            // 审批通过:按事项类型执行业务联动(出证/更新企业信息/禁用证照),返回新签发证照号
            issuedNo = applyApprovalPassed(record, matter);
            record.setRemark(null); // 办结后清空历史退回意见
        } else if ("REJECTED".equals(status)) {
            record.setStatus("REJECTED");
            record.setRemark(StrUtil.blankToDefault(remark, null)); // 落退回原因,供企业端查看
        } else if ("REPAIR".equals(status)) {
            record.setStatus("REPAIR");
            record.setRemark(StrUtil.blankToDefault(remark, "材料需补充,请补正后重新提交"));
        }
        record.setFinishTime(LocalDateTime.now());
        recordMapper.updateById(record);
        // 办结清空 remark:updateById 忽略 null,需 wrapper 强制写 NULL
        if (("PASSED".equals(status) || "APPROVED".equals(status)) && StrUtil.isNotBlank(record.getRemark())) {
            com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<EntApplicationRecord> uw =
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<EntApplicationRecord>()
                            .eq(EntApplicationRecord::getId, record.getId())
                            .set(EntApplicationRecord::getRemark, null);
            recordMapper.update(null, uw);
        }
        log.info("申报 {} 审批结果: {}, 事项: {}, 签发证照号: {}", recordId, status, matter == null ? "" : matter.getMatterName(), issuedNo);
        // 定稿后上报监管端(大屏按真实审批结果更新;失败静默不影响回调)
        supervisionReportService.report(record, matter);
        // 定稿站内信(办结/驳回/退回实时可感, 消息中心聚合)
        sendResultMessage(record, matter, status, remark, issuedNo);
        return issuedNo;
    }

    /** 定稿结果推送企业站内信 */
    private void sendResultMessage(EntApplicationRecord record, EntMatterInfo matter, String status,
                                   String remark, String issuedNo) {
        String matterName = matter == null ? "该申报" : matter.getMatterName();
        String title;
        String content;
        if ("PASSED".equals(status) || "APPROVED".equals(status)) {
            title = "办件已办结";
            content = "您的「" + matterName + "」申报已办结。"
                    + (StrUtil.isNotBlank(issuedNo) ? "已生成电子证照(证照号 " + issuedNo + ")，可在「证照管理」中查看。" : "");
        } else if ("REJECTED".equals(status)) {
            title = "办件未通过";
            content = "您的「" + matterName + "」申报未通过审批。"
                    + (StrUtil.isBlank(remark) ? "" : "\n审批意见：" + remark);
        } else if ("REPAIR".equals(status)) {
            title = "申报退回补正";
            content = "您的「" + matterName + "」申报已退回补正。"
                    + (StrUtil.isBlank(remark) ? "" : "\n补正意见：" + remark);
        } else {
            return;
        }
        try {
            entMessageService.send(record.getEnterpriseId(), "APPROVAL", title, content);
        } catch (Exception e) {
            log.warn("办结站内信发送失败(忽略) recordId={} err={}", record.getId(), e.getMessage());
        }
    }

    /**
     * 按审批实例ID查申报记录(审批端定稿回调用;无权限上下文,仅服务间调用)
     */
    public EntApplicationRecord findByInstanceId(Long instanceId) {
        if (instanceId == null) {
            return null;
        }
        return recordMapper.selectOne(new LambdaQueryWrapper<EntApplicationRecord>()
                .eq(EntApplicationRecord::getApprovalInstanceId, instanceId)
                .last("LIMIT 1"));
    }

    /**
     * 审批端定稿回调(按 instanceId 定位申报记录,处理办结/驳回/退回)
     * 与原 handleApprovalResult 的区别:无 LoginUser,审批端 HTTP 回调时使用
     */
    @Transactional
    public String handleApprovalResultByInstance(Long instanceId, String status, String remark) {
        EntApplicationRecord record = findByInstanceId(instanceId);
        if (record == null) {
            throw new BusinessException("未找到关联该审批实例的申报记录: instanceId=" + instanceId);
        }
        return handleApprovalResult(record.getId(), status, remark);
    }

    /**
     * 审批通过后的业务联动
     * - 新办类(食品/卫生/烟草/药品/排污许可):生成对应电子证照
     * - 企业开办:生成营业执照
     * - 变更类(企业变更/股权变更):同步更新企业信息
     * - 注销类:禁用对应证照
     * - 延续类:更新证照到期日期
     * @return 新签发证照的证照号(未签发返回 null),供审批端回写办结台账保持两端号一致
     */
    private String applyApprovalPassed(EntApplicationRecord record, EntMatterInfo matter) {
        if (matter == null) {
            return null;
        }
        Long enterpriseId = record.getEnterpriseId();
        Map<String, Object> form = parseFormSafe(record.getFormData());
        String matterName = matter.getMatterName();
        Long matterId = matter.getId();
        LocalDate today = LocalDate.now();
        String issuedNo = null;

        // 1. 许可证新办类:生成电子证照
        if (matterName.contains("新办") || matterName.contains("开办")) {
            String licenseType = mapLicenseType(matterName);
            if (licenseType != null) {
                String licenseNo = genLicenseNo(matterId);
                LocalDate expiry = licenseType.contains("营业执照") ? today.plusYears(20) : today.plusYears(5);
                licenseService.addLicense(enterpriseId, licenseType, licenseNo,
                        enterpriseName(enterpriseId) + licenseType, today, expiry, null);
                issuedNo = licenseNo;
                log.info("审批通过:企业 {} 生成证照 {} 证照号 {}", enterpriseId, licenseType, licenseNo);
            }
        }
        // 2. 许可证延续类:旧证作废 + 换发新证(同类型,新证号,有效期+5年;避免重复堆证/续错证)
        else if (matterName.contains("延续")) {
            String licenseNo = form.get("licenseNo") == null ? null : String.valueOf(form.get("licenseNo"));
            if (StrUtil.isNotBlank(licenseNo)) {
                EnterpriseLicense lic = licenseMapper.selectOne(
                        new LambdaQueryWrapper<EnterpriseLicense>()
                                .eq(EnterpriseLicense::getEnterpriseId, enterpriseId)
                                .eq(EnterpriseLicense::getLicenseNo, licenseNo)
                                .last("LIMIT 1"));
                // 类型校验:延续的必须是该事项对应证照类型的证(防止"食品延续"误续卫生证号)
                String expectType = mapLicenseType(matterName);
                if (lic != null && expectType != null && expectType.equals(lic.getLicenseType())) {
                    // 旧证作废(保留历史,前端标"已失效"),换发同类型新证
                    lic.setStatus(0);
                    lic.setVoidReason("RENEW_EXTEND");
                    licenseMapper.updateById(lic);
                    String newNo = genLicenseNo(matterId);
                    licenseService.addLicense(enterpriseId, lic.getLicenseType(), newNo,
                            enterpriseName(enterpriseId) + lic.getLicenseType(), today, today.plusYears(5), null);
                    issuedNo = newNo;
                    log.info("审批通过:证照 {} 延续换发新证 {} 有效期至 {}", licenseNo, newNo, today.plusYears(5));
                } else if (lic != null) {
                    log.warn("延续证照类型不匹配,跳过: 原[{}] 期望[{}]", lic.getLicenseType(), expectType);
                }
            }
        }
        // 3. 注销类: 许可注销(有证号且目标证型明确)→单证作废; 企业整体注销(企业简易注销,无具体证型)→整企注销+名下证照批量失效
        else if (matterName.contains("注销")) {
            String licenseNo = form.get("licenseNo") == null ? null : String.valueOf(form.get("licenseNo"));
            String expectType = mapLicenseType(matterName);
            if (StrUtil.isNotBlank(licenseNo) && expectType != null) {
                EnterpriseLicense lic = licenseMapper.selectOne(
                        new LambdaQueryWrapper<EnterpriseLicense>()
                                .eq(EnterpriseLicense::getEnterpriseId, enterpriseId)
                                .eq(EnterpriseLicense::getLicenseNo, licenseNo)
                                .last("LIMIT 1"));
                if (lic != null && expectType.equals(lic.getLicenseType())) {
                    lic.setStatus(0);
                    lic.setVoidReason("CANCEL_SELF");
                    licenseMapper.updateById(lic);
                    log.info("审批通过:证照 {} 已注销禁用", licenseNo);
                } else if (lic != null) {
                    log.warn("注销证照类型不匹配,跳过: 原[{}] 期望[{}]", lic.getLicenseType(), expectType);
                }
            } else {
                cancelWholeEnterprise(enterpriseId);
            }
        }
        // 3.1 补办类(证照遗失/损毁): 按原证类型补发新证(副本)
        else if (matterName.contains("补办")) {
            String expectType = mapLicenseType(matterName);
            if (expectType != null) {
                String newNo = genLicenseNo(matterId);
                licenseService.addLicense(enterpriseId, expectType, newNo,
                        enterpriseName(enterpriseId) + expectType + "(补办)", today, today.plusYears(5), null);
                issuedNo = newNo;
                log.info("审批通过:证照补办,企业 {} 补发新证 {}", enterpriseId, newNo);
            }
        }
        // 4. 变更类: 许可变更(目标证型明确)→原证换发(体现已变更); 企业/股权变更→更新企业登记信息(经营范围/注册资本落库)
        else if (matterName.contains("变更") || matterName.contains("股权")) {
            String expectType = mapLicenseType(matterName);
            if (expectType != null) {
                // 许可类变更(食品经营许可变更等): 原证作废换发同类型新证(变更后证照)
                String licenseNo = form.get("licenseNo") == null ? null : String.valueOf(form.get("licenseNo"));
                EnterpriseLicense old = StrUtil.isNotBlank(licenseNo)
                        ? licenseMapper.selectOne(new LambdaQueryWrapper<EnterpriseLicense>()
                                .eq(EnterpriseLicense::getEnterpriseId, enterpriseId)
                                .eq(EnterpriseLicense::getLicenseNo, licenseNo)
                                .last("LIMIT 1"))
                        : null;
                if (old != null && expectType.equals(old.getLicenseType())) {
                    LocalDate oldExpiry = old.getExpiryDate();
                    old.setStatus(0);
                    old.setVoidReason("RENEW_CHANGE");
                    licenseMapper.updateById(old);
                    String newNo = genLicenseNo(matterId);
                    licenseService.addLicense(enterpriseId, expectType, newNo,
                            enterpriseName(enterpriseId) + expectType, today,
                            oldExpiry != null ? oldExpiry : today.plusYears(5), null);
                    issuedNo = newNo;
                    log.info("审批通过:证照变更换发,原[{}] 新[{}]", licenseNo, newNo);
                } else {
                    String newNo = genLicenseNo(matterId);
                    licenseService.addLicense(enterpriseId, expectType, newNo,
                            enterpriseName(enterpriseId) + expectType, today, today.plusYears(5), null);
                    issuedNo = newNo;
                }
            } else {
                // 企业信息变更(名称/法人/地址/经营范围/注册资本落库)
                EnterpriseInfo info = enterpriseInfoMapper.selectById(enterpriseId);
                if (info != null) {
                    if (form.containsKey("newEnterpriseName") && StrUtil.isNotBlank(str(form.get("newEnterpriseName")))) {
                        info.setEnterpriseName(str(form.get("newEnterpriseName")));
                    }
                    if (form.containsKey("newLegalPerson") && StrUtil.isNotBlank(str(form.get("newLegalPerson")))) {
                        info.setLegalPerson(str(form.get("newLegalPerson")));
                    }
                    if (form.containsKey("newRegisterAddress") && StrUtil.isNotBlank(str(form.get("newRegisterAddress")))) {
                        info.setRegisterAddress(str(form.get("newRegisterAddress")));
                    }
                    if (form.containsKey("newBusinessScope") && StrUtil.isNotBlank(str(form.get("newBusinessScope")))) {
                        info.setBusinessScope(str(form.get("newBusinessScope")));
                    }
                    if (form.containsKey("newRegisterCapital") && str(form.get("newRegisterCapital")) != null) {
                        try {
                            info.setRegisterCapital(new BigDecimal(str(form.get("newRegisterCapital"))));
                        } catch (Exception ignore) { }
                    }
                    enterpriseInfoMapper.updateById(info);
                    log.info("审批通过:企业 {} 登记信息已更新", enterpriseId);
                }
            }
        }
        // 5. 经营期限延期: 更新企业登记的经营期限至(business_term_until)
        else if (matterName.contains("经营期限")) {
            EnterpriseInfo info = enterpriseInfoMapper.selectById(enterpriseId);
            if (info != null && form.get("newTerm") != null) {
                try {
                    int years = new BigDecimal(str(form.get("newTerm"))).intValue();
                    if (years > 0) {
                        info.setBusinessTermUntil(today.plusYears(years));
                        enterpriseInfoMapper.updateById(info);
                        log.info("审批通过:企业 {} 经营期限延期至 {}", enterpriseId, info.getBusinessTermUntil());
                    }
                } catch (Exception e) {
                    log.warn("经营期限延期失败(忽略) enterpriseId={}", enterpriseId);
                }
            }
        }
        // 6. 资质认定类(如"高新技术企业认定"):通过后颁发对应资质认定证书
        //    证照类型名 = 事项名"认定"→"证书"(高新技术企业认定 → 高新技术企业证书),有效期3年,证照号 GR 开头
        else if (matterName.contains("认定")) {
            String certType = matterName.replace("认定", "证书");
            String certNo = "GR" + IdUtil.randomUUID().substring(0, 12).toUpperCase();
            licenseService.addLicense(enterpriseId, certType, certNo,
                    enterpriseName(enterpriseId) + certType, today, today.plusYears(3), null);
            issuedNo = certNo;
            log.info("审批通过:企业 {} 颁发资质证书 {} 证书号 {}", enterpriseId, certType, certNo);
        }
        // 7. 扶持补贴类(稳岗返还补贴/增值税减免确认等): 记录补贴/减免发放
        else if (matterName.contains("补贴") || matterName.contains("减免") || matterName.contains("稳岗")) {
            recordSubsidyPayment(record, matter, form, enterpriseId);
        }
        return issuedNo;
    }

    /** 企业整体注销: 企业状态置0(注销), 名下全部有效证照批量置失效 */
    private void cancelWholeEnterprise(Long enterpriseId) {
        EnterpriseInfo info = enterpriseInfoMapper.selectById(enterpriseId);
        if (info != null && !Integer.valueOf(0).equals(info.getStatus())) {
            info.setStatus(0);
            enterpriseInfoMapper.updateById(info);
        }
        List<EnterpriseLicense> actives = licenseMapper.selectList(new LambdaQueryWrapper<EnterpriseLicense>()
                .eq(EnterpriseLicense::getEnterpriseId, enterpriseId)
                .eq(EnterpriseLicense::getStatus, 1));
        if (!actives.isEmpty()) {
            for (EnterpriseLicense l : actives) {
                l.setStatus(0);
                l.setVoidReason("CANCEL_ENT");
                licenseMapper.updateById(l);
            }
            log.info("企业注销:企业 {} 及名下 {} 张证照已全部注销", enterpriseId, actives.size());
        } else {
            log.info("企业注销:企业 {} 状态已置注销", enterpriseId);
        }
    }

    /** 补贴/减免办结记录发放: 稳岗返还取上年失业保险缴费额作返还额; 增值税减免等无缴费额则记录减免资格确认 */
    private void recordSubsidyPayment(EntApplicationRecord record, EntMatterInfo matter,
                                      Map<String, Object> form, Long enterpriseId) {
        SubsidyPayment sp = new SubsidyPayment();
        sp.setEnterpriseId(enterpriseId);
        sp.setMatterId(matter.getId());
        sp.setRecordId(record.getId());
        sp.setSubsidyType(matter.getMatterName());
        sp.setStatus("PAID");
        sp.setPayTime(LocalDateTime.now());
        sp.setBankName(str(form.get("bankName")));
        sp.setBankAccount(str(form.get("bankAccount")));
        BigDecimal amount = BigDecimal.ZERO;
        if (form.get("lastYearInsuranceAmount") != null) {
            try {
                amount = new BigDecimal(str(form.get("lastYearInsuranceAmount")));
            } catch (Exception ignore) { }
        } else if (form.get("annualSales") != null) {
            try {
                amount = new BigDecimal(str(form.get("annualSales")));
            } catch (Exception ignore) { }
        }
        sp.setAmount(amount);
        subsidyPaymentMapper.insert(sp);
        log.info("审批通过:企业 {} 补贴/减免记录 类型[{}] 金额[{}]元", enterpriseId, matter.getMatterName(), amount);
    }

    /** 事项名称 → 证照类型映射 */
    /**
     * 按审批实例号查该申报已上传的材料清单（供审批端复核材料，服务间调用）。
     * 审批端原本看不到申报人上传的材料，导致材料内容无人核对；此接口补齐该链路。
     */
    public List<EntApplicationMaterial> materialsByInstance(Long instanceId) {
        EntApplicationRecord record = findByInstanceId(instanceId);
        if (record == null) return new ArrayList<>();
        return materialMapper.selectList(new LambdaQueryWrapper<EntApplicationMaterial>()
                .eq(EntApplicationMaterial::getRecordId, record.getId())
                .orderByAsc(EntApplicationMaterial::getId));
    }

    /**
     * 提交前材料齐全性校验（服务端兜底，与前端同口径）。
     * 事项材料清单里每一条，满足以下任一即可：
     *   ① 已上传（ent_application_material.material_name 匹配，忽略末尾括号标注）
     *   ② 免证办（材料名含 营业执照 / 许可 / 资质，且企业持有对应类型有效电子证照）
     * 缺失则抛业务异常并列出缺哪些材料。
     */
    private void checkMaterialsComplete(LoginUser user, EntMatterInfo matter, Long recordId) {
        List<String> required = JSONUtil.toList(matter.getMaterials(), String.class);
        if (required == null || required.isEmpty()) return;   // 该事项无材料要求
        List<String> uploadedNames = materialMapper.selectList(
                        new LambdaQueryWrapper<EntApplicationMaterial>()
                                .eq(EntApplicationMaterial::getRecordId, recordId))
                .stream()
                .map(EntApplicationMaterial::getMaterialName)
                .filter(n -> n != null && !n.isEmpty())
                .map(this::stripMaterialNote)
                .collect(Collectors.toList());
        List<EnterpriseLicense> actives = licenseMapper.selectList(new LambdaQueryWrapper<EnterpriseLicense>()
                .eq(EnterpriseLicense::getEnterpriseId, user.getEnterpriseId())
                .eq(EnterpriseLicense::getStatus, 1));
        List<String> missing = new ArrayList<>();
        for (String raw : required) {
            String name = stripMaterialNote(raw);
            if (StrUtil.isBlank(name)) continue;
            if (uploadedNames.contains(name)) continue;        // ① 已上传
            if (isAutoFillByLicense(name, actives)) continue;  // ② 免证办自动核验
            missing.add(name);
        }
        if (!missing.isEmpty()) {
            throw new BusinessException("以下材料尚未提交，无法提交申报：" + String.join("、", missing)
                    + "；请在上传材料步骤补齐后重试（企业已有对应电子证照的，可走免证办自动核验）");
        }
    }

    /** 去掉材料名末尾的标注：如 "营业执照(电子证照,免证办)" → "营业执照"；"(变更地址时)" 这类条件标注同样去掉 */
    private String stripMaterialNote(String name) {
        if (name == null) return null;
        return name.replaceAll("(\\([^)]*\\))+\\s*$", "").trim();
    }

    /** 免证办判定：材料名含 营业执照/许可/资质 关键词，且企业持有对应类型的有效电子证照 */
    private boolean isAutoFillByLicense(String materialName, List<EnterpriseLicense> actives) {
        String kw = null;
        if (materialName.contains("营业执照")) kw = "营业执照";
        else if (materialName.contains("许可")) kw = "许可";
        else if (materialName.contains("资质")) kw = "资质";
        if (kw == null) return false;
        final String key = kw;
        return actives.stream().anyMatch(l -> l.getLicenseType() != null && l.getLicenseType().contains(key));
    }

    /**
     * 材料相符性弱校验：把"材料存疑"标记写入表单数据（__materialSuspect=true）。
     * 判定：材料名（去标注）与原始文件名无任何 2 字交集 → 存疑（例：要求"从业人员健康证"，实际文件名为"简历.pdf"）。
     * 说明：无 OCR 时无法校验材料内容本身，这是可做的弱校验；存疑不阻断提交，
     *       但审批端据此不"直达办结"，改走人工受理核对。
     */
    private void markMaterialSuspectIfAny(EntApplicationRecord record, Long recordId) {
        List<EntApplicationMaterial> mats = materialMapper.selectList(
                new LambdaQueryWrapper<EntApplicationMaterial>()
                        .eq(EntApplicationMaterial::getRecordId, recordId));
        if (mats.isEmpty()) return;
        for (EntApplicationMaterial m : mats) {
            if (isMaterialSuspect(m.getMaterialName(), m.getOriginalName())) {
                cn.hutool.json.JSONObject form = StrUtil.isBlank(record.getFormData())
                        ? new cn.hutool.json.JSONObject()
                        : JSONUtil.parseObj(record.getFormData());
                form.set("__materialSuspect", true);
                record.setFormData(form.toString());
                log.warn("材料存疑(材料名与文件名不符): recordId={} 材料={} 文件={}",
                        recordId, m.getMaterialName(), m.getOriginalName());
                return;
            }
        }
    }

    /** 材料名与原始文件名是否有 2 字以上交集；无交集、或拿不到原文件名 → 判为存疑 */
    private boolean isMaterialSuspect(String materialName, String originalName) {
        String name = stripMaterialNote(materialName);
        if (StrUtil.isBlank(name)) return false;
        if (StrUtil.isBlank(originalName)) return true;
        String file = originalName.toLowerCase();
        String lower = name.toLowerCase();
        for (int i = 0; i + 2 <= lower.length(); i++) {
            if (file.contains(lower.substring(i, i + 2))) return false;   // 有交集 → 不存疑
        }
        return true;
    }

    private String mapLicenseType(String matterName) {
        if (matterName.contains("食品经营")) return "食品经营许可证";
        if (matterName.contains("卫生")) return "卫生许可证";
        if (matterName.contains("烟草")) return "烟草专卖零售许可证";
        if (matterName.contains("药品")) return "药品经营许可证";
        if (matterName.contains("排污")) return "排污许可证";
        if (matterName.contains("开办")) return "营业执照";
        return null;
    }

    /** 生成模拟证照编号 */
    private String genLicenseNo(Long matterId) {
        String prefix = matterId == 1 ? "91" : "";
        return prefix + IdUtil.randomUUID().substring(0, 16).toUpperCase();
    }

    private String enterpriseName(Long enterpriseId) {
        EnterpriseInfo info = enterpriseInfoMapper.selectById(enterpriseId);
        return info == null ? "" : info.getEnterpriseName();
    }

    private String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private Map<String, Object> parseFormSafe(String formJson) {
        try {
            return JSONUtil.parseObj(formJson);
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    /** 本地模拟审批进度(开发期审批端不在线时展示用) */
    private ApprovalProgressResp mockProgress(Long instanceId, LocalDateTime submitTime) {
        ApprovalProgressResp resp = new ApprovalProgressResp();
        resp.setInstanceId(instanceId);
        resp.setStatus("APPROVING");
        resp.setCurrentNode("初审");
        String submit = submitTime == null ? LocalDateTime.now().toString() : submitTime.toString();
        resp.setSubmitTime(submit);
        List<ApprovalProgressResp.Node> nodes = new ArrayList<>();
        ApprovalProgressResp.Node n1 = new ApprovalProgressResp.Node();
        n1.setName("提交"); n1.setStatus("DONE"); n1.setHandleTime(submit);
        ApprovalProgressResp.Node n2 = new ApprovalProgressResp.Node();
        n2.setName("初审"); n2.setStatus("CURRENT"); n2.setAssignee("张审批");
        ApprovalProgressResp.Node n3 = new ApprovalProgressResp.Node();
        n3.setName("复审"); n3.setStatus("PENDING");
        ApprovalProgressResp.Node n4 = new ApprovalProgressResp.Node();
        n4.setName("办结"); n4.setStatus("PENDING");
        nodes.add(n1); nodes.add(n2); nodes.add(n3); nodes.add(n4);
        resp.setNodes(nodes);
        return resp;
    }

    // ============ 内部方法 ============

    private EntApplicationRecord checkOwnRecord(LoginUser user, Long recordId) {
        EntApplicationRecord record = recordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException("申报记录不存在");
        }
        if (!record.getEnterpriseId().equals(user.getEnterpriseId())) {
            throw new BusinessException(403, "无权操作该申报记录");
        }
        return record;
    }

    private void requireEnterprise(LoginUser user) {
        if (user.getEnterpriseId() == null) {
            throw new BusinessException("当前账号未绑定企业,无法申报");
        }
        EnterpriseInfo info = enterpriseInfoMapper.selectById(user.getEnterpriseId());
        if (info != null && Integer.valueOf(0).equals(info.getStatus())) {
            throw new BusinessException(403, "该企业已注销,无法办理申报业务");
        }
    }

    private EnterpriseInfo enterpriseInfo(Long id) {
        EnterpriseInfo info = enterpriseInfoMapper.selectById(id);
        if (info == null) {
            throw new BusinessException("企业信息不存在");
        }
        return info;
    }

    private String matterName(Long matterId) {
        if (matterId == null) {
            return null;
        }
        EntMatterInfo m = matterMapper.selectById(matterId);
        return m == null ? null : m.getMatterName();
    }

    private String matterCode(Long matterId) {
        if (matterId == null) {
            return null;
        }
        EntMatterInfo m = matterMapper.selectById(matterId);
        return m == null ? null : m.getMatterCode();
    }

    /** 记录号: QY + yyyyMMdd + 4位随机 */
    private String genRecordCode() {
        String date = cn.hutool.core.date.DateUtil.format(cn.hutool.core.date.DateUtil.date(), "yyyyMMdd");
        return CODE_PREFIX + date + IdUtil.randomUUID().substring(0, 4).toUpperCase();
    }
}
