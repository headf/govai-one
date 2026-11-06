package com.govai.enterprise.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.govai.enterprise.common.BusinessException;
import com.govai.enterprise.common.LoginUser;
import com.govai.enterprise.dto.ApprovalSubmitReq;
import com.govai.enterprise.dto.ApprovalSubmitResp;
import com.govai.enterprise.dto.MatterVO;
import com.govai.enterprise.dto.PackageSubmitReq;
import com.govai.enterprise.dto.PackageSubmitResp;
import com.govai.enterprise.dto.PackageVO;
import com.govai.enterprise.entity.EntApplicationMaterial;
import com.govai.enterprise.entity.EntApplicationRecord;
import com.govai.enterprise.entity.EntMatterInfo;
import com.govai.enterprise.entity.EntPackage;
import com.govai.enterprise.entity.EntPackageItem;
import com.govai.enterprise.entity.EnterpriseInfo;
import com.govai.enterprise.feign.ApprovalClient;
import com.govai.enterprise.mapper.EntApplicationMaterialMapper;
import com.govai.enterprise.mapper.EntApplicationRecordMapper;
import com.govai.enterprise.mapper.EntMatterInfoMapper;
import com.govai.enterprise.mapper.EntPackageItemMapper;
import com.govai.enterprise.mapper.EntPackageMapper;
import com.govai.enterprise.mapper.EnterpriseInfoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 一件事套餐服务(套餐浏览 + 一表申报合并 + 提交拆分多事项)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PackageService {

    private final EntPackageMapper packageMapper;
    private final EntPackageItemMapper itemMapper;
    private final EntMatterInfoMapper matterMapper;
    private final EntApplicationRecordMapper recordMapper;
    private final EntApplicationMaterialMapper materialMapper;
    private final FileStorageService fileStorage;
    private final EnterpriseInfoMapper enterpriseInfoMapper;
    private final ApprovalClient approvalClient;
    private final SupervisionReportService supervisionReportService;

    private static final String CODE_PREFIX = "TC";

    /**
     * 套餐列表(含事项明细)
     */
    public List<PackageVO> listPackages() {
        List<EntPackage> packages = packageMapper.selectList(new LambdaQueryWrapper<EntPackage>()
                .eq(EntPackage::getStatus, 1)
                .orderByAsc(EntPackage::getId));
        List<PackageVO> result = new ArrayList<>();
        for (EntPackage p : packages) {
            result.add(toVO(p, true));
        }
        return result;
    }

    /**
     * 套餐详情(合并表单 + 去重材料)
     */
    public PackageVO detail(Long packageId) {
        EntPackage p = packageMapper.selectById(packageId);
        if (p == null || (p.getStatus() != null && p.getStatus() == 0)) {
            throw new BusinessException("套餐不存在或已下线");
        }
        return toVO(p, true);
    }

    /**
     * 套餐材料上传:一表申报的合并清单在提交前先落到「材料库」存文件,
     * 因拆分出的子申报记录要等 submit 才创建,故先返回文件URL,提交时再按材料名批量关联到各子申报。
     * 返回可直接展示的 {materialName, fileUrl}。
     */
    public Map<String, String> uploadMaterial(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的文件");
        }
        Map<String, String> resp = new HashMap<>();
        resp.put("fileUrl", fileStorage.store(file, "package"));
        resp.put("materialName", StrUtil.blankToDefault(file.getOriginalFilename(), "未命名文件"));
        return resp;
    }

    /**
     * 一表申报提交:合并表单拆分到每个事项,生成多条独立申报并进入审批,
     * 按材料名把已备齐材料批量关联到各子申报记录。
     */
    @Transactional
    public PackageSubmitResp submit(LoginUser user, PackageSubmitReq req) {
        if (user.getEnterpriseId() == null) {
            throw new BusinessException("当前账号未绑定企业,无法申报");
        }
        EntPackage p = packageMapper.selectById(req.getPackageId());
        if (p == null || (p.getStatus() != null && p.getStatus() == 0)) {
            throw new BusinessException("套餐不存在或已下线");
        }
        List<EntPackageItem> items = itemMapper.selectList(new LambdaQueryWrapper<EntPackageItem>()
                .eq(EntPackageItem::getPackageId, p.getId())
                .orderByAsc(EntPackageItem::getSortOrder));
        if (items.isEmpty()) {
            throw new BusinessException("套餐未配置事项");
        }
        // 合并清单里每份材料先解析出 (名称 -> 文件URL),供按材料名匹配落库
        Map<String, String> provided = new LinkedHashMap<>();
        if (req.getMaterials() != null) {
            for (PackageSubmitReq.Material m : req.getMaterials()) {
                if (m.getMaterialName() != null && StrUtil.isNotBlank(m.getFileUrl())) {
                    provided.putIfAbsent(m.getMaterialName().trim(), m.getFileUrl());
                }
            }
        }

        String formJson = JSONUtil.toJsonStr(req.getFormData());
        PackageSubmitResp resp = new PackageSubmitResp();
        List<PackageSubmitResp.Item> records = new ArrayList<>();
        int idx = 0;
        // 第一遍:先为每个子事项按「子事项材料清单」分配匹配的材料,并记录哪些材料已被消费
        Set<String> consumed = new LinkedHashSet<>();
        Map<EntMatterInfo, EntApplicationRecord> matterRecords = new LinkedHashMap<>();
        for (EntPackageItem item : items) {
            EntMatterInfo matter = matterMapper.selectById(item.getMatterId());
            if (matter == null) {
                continue;
            }
            EntApplicationRecord record = new EntApplicationRecord();
            record.setRecordCode(genRecordCode());
            record.setEnterpriseId(user.getEnterpriseId());
            record.setMatterId(matter.getId());
            record.setOperatorId(user.getUserId());
            record.setFormData(formJson);
            record.setStatus("PENDING");
            record.setSubmitTime(LocalDateTime.now());
            recordMapper.insert(record);
            matterRecords.put(matter, record);

            for (String requiredName : parseList(matter.getMaterials())) {
                String url = provided.get(requiredName.trim());
                if (url != null) {
                    insertMaterial(record.getId(), requiredName.trim(), url);
                    consumed.add(requiredName.trim());
                }
            }

            // 调审批端提交该子事项(套餐拆分后各子项独立进入审批流程),失败则本地模拟 5001 降级
            submitToApproval(user, record, matter, req.getFormData());

            PackageSubmitResp.Item ri = new PackageSubmitResp.Item();
            ri.setRecordId(record.getId());
            ri.setRecordCode(record.getRecordCode());
            ri.setMatterId(matter.getId());
            ri.setMatterName(matter.getMatterName());
            ri.setStatus(record.getStatus());
            records.add(ri);
            idx++;
        }
        // 第二遍:合并清单里不属于任何子事项的多余材料(如交回证照等补充件),挂到首个申报,保证不丢
        if (!matterRecords.isEmpty() && consumed.size() < provided.size()) {
            EntApplicationRecord first = matterRecords.values().iterator().next();
            for (Map.Entry<String, String> e : provided.entrySet()) {
                if (!consumed.contains(e.getKey())) {
                    insertMaterial(first.getId(), e.getKey(), e.getValue());
                }
            }
        }
        resp.setRecords(records);
        return resp;
    }

    /**
     * 套餐拆分出的单个子事项提交审批端(契约接口4),并更新本地记录状态;
     * 审批端不可用时本地模拟(5001),保证套餐主链路可独立演示。
     */
    private void submitToApproval(LoginUser user, EntApplicationRecord record, EntMatterInfo matter,
                                  Map<String, Object> formData) {
        try {
            EnterpriseInfo enterprise = enterpriseInfoMapper.selectById(user.getEnterpriseId());
            ApprovalSubmitReq approvalReq = new ApprovalSubmitReq();
            approvalReq.setTemplateCode(matter.getMatterCode());
            approvalReq.setMatterId(matter.getId());
            approvalReq.setApplicantName(user.getRealName());
            approvalReq.setApplicantId(enterprise == null ? null : enterprise.getCreditCode());
            approvalReq.setApplicantType("ENTERPRISE");
            approvalReq.setFormData(formData);
            com.govai.enterprise.common.Result<ApprovalSubmitResp> r = approvalClient.submit(approvalReq);
            if (r != null && r.getData() != null && r.getData().getInstanceId() != null) {
                record.setApprovalInstanceId(r.getData().getInstanceId());
                if (r.getData().getStatus() != null) {
                    record.setStatus(r.getData().getStatus());
                }
                recordMapper.updateById(record);
            }
        } catch (Exception e) {
            log.warn("套餐子项调审批端不可用,本地模拟(5001): matter={} err={}",
                    matter == null ? "" : matter.getMatterName(), e.getMessage());
            record.setApprovalInstanceId(5001L);
            record.setStatus("APPROVING");
            recordMapper.updateById(record);
        }
        // 上报监管端(大屏真实数据源;失败静默不影响套餐主链路)
        try {
            supervisionReportService.report(record, matter);
        } catch (Exception e) {
            log.warn("套餐子项上报监管端失败(忽略): {}", e.getMessage());
        }
    }

    private void insertMaterial(Long recordId, String materialName, String fileUrl) {
        EntApplicationMaterial m = new EntApplicationMaterial();
        m.setRecordId(recordId);
        m.setMaterialName(materialName);
        m.setFileUrl(fileUrl);
        m.setFileType(inferContentType(fileUrl));
        m.setOcrStatus(0);
        m.setUploadTime(LocalDateTime.now());
        materialMapper.insert(m);
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

    // ============ 组装 VO(合并表单/材料) ============

    private PackageVO toVO(EntPackage p, boolean withItems) {
        PackageVO vo = new PackageVO();
        vo.setId(p.getId());
        vo.setPackageCode(p.getPackageCode());
        vo.setPackageName(p.getPackageName());
        vo.setDescription(p.getDescription());
        if (!withItems) {
            return vo;
        }
        List<EntPackageItem> items = itemMapper.selectList(new LambdaQueryWrapper<EntPackageItem>()
                .eq(EntPackageItem::getPackageId, p.getId())
                .orderByAsc(EntPackageItem::getSortOrder));

        List<PackageVO.PackageItemVO> itemVOs = new ArrayList<>();
        // 合并字段去重:共用字段只填一次
        Map<String, MatterVO.FormFieldConfig> mergedFieldMap = new LinkedHashMap<>();
        Set<String> mergedMaterials = new LinkedHashSet<>();
        Set<String> mergedSteps = new LinkedHashSet<>();
        int maxDeadline = 0;

        for (EntPackageItem item : items) {
            EntMatterInfo matter = matterMapper.selectById(item.getMatterId());
            if (matter == null) {
                continue;
            }
            PackageVO.PackageItemVO iv = new PackageVO.PackageItemVO();
            iv.setMatterId(matter.getId());
            iv.setMatterCode(matter.getMatterCode());
            iv.setMatterName(matter.getMatterName());
            iv.setDepartment(matter.getDepartment());
            iv.setDeadlineDays(matter.getDeadlineDays());
            iv.setFormConfig(parseForm(matter.getFormConfig()));
            iv.setMaterials(parseList(matter.getMaterials()));
            itemVOs.add(iv);

            if (matter.getFormConfig() != null) {
                for (MatterVO.FormFieldConfig f : parseForm(matter.getFormConfig())) {
                    mergedFieldMap.putIfAbsent(f.getKey(), f);
                }
            }
            if (matter.getMaterials() != null) {
                mergedMaterials.addAll(parseList(matter.getMaterials()));
            }
            if (matter.getProcessSteps() != null) {
                mergedSteps.addAll(parseList(matter.getProcessSteps()));
            }
            if (matter.getDeadlineDays() != null && matter.getDeadlineDays() > maxDeadline) {
                maxDeadline = matter.getDeadlineDays();
            }
        }

        vo.setItems(itemVOs);
        vo.setMergedForm(new ArrayList<>(mergedFieldMap.values()));
        vo.setMergedMaterials(new ArrayList<>(mergedMaterials));
        vo.setProcessSteps(new ArrayList<>(mergedSteps));
        vo.setTotalDeadlineDays(maxDeadline);
        return vo;
    }

    private List<MatterVO.FormFieldConfig> parseForm(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return JSONUtil.toList(json, MatterVO.FormFieldConfig.class);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private List<String> parseList(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return JSONUtil.toList(json, String.class);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String genRecordCode() {
        String date = cn.hutool.core.date.DateUtil.format(cn.hutool.core.date.DateUtil.date(), "yyyyMMdd");
        return CODE_PREFIX + date + IdUtil.randomUUID().substring(0, 4).toUpperCase();
    }
}
