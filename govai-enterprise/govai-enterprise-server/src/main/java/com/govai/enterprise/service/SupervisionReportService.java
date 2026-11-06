package com.govai.enterprise.service;

import com.govai.enterprise.dto.SupervisionReportReq;
import com.govai.enterprise.entity.EntApplicationRecord;
import com.govai.enterprise.entity.EnterpriseInfo;
import com.govai.enterprise.entity.EntMatterInfo;
import com.govai.enterprise.feign.SupervisionClient;
import com.govai.enterprise.mapper.EnterpriseInfoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * 申报上报监管端服务
 * 企业端申报在"提交"与"审批定稿"后上报一条到监管端 approval_data_cache,
 * 让大屏/风险预警使用企业真实申报数据。上报失败/监管端离线时静默跳过,不影响申报主链路。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SupervisionReportService {

    private final SupervisionClient supervisionClient;
    private final EnterpriseInfoMapper enterpriseInfoMapper;

    /**
     * 上报一条申报(内部按 record.enterpriseId 拉企业档案)
     *
     * @param record 申报记录(状态已更新)
     * @param matter 事项(取事项名;可为 null)
     */
    public void report(EntApplicationRecord record, EntMatterInfo matter) {
        try {
            if (record == null) {
                return;
            }
            EnterpriseInfo ent = record.getEnterpriseId() == null ? null
                    : enterpriseInfoMapper.selectById(record.getEnterpriseId());
            SupervisionReportReq req = new SupervisionReportReq();
            // 审批实例号:真实实例直接使用;演示哨兵(5001/审批端离线)或为空时改用申报记录ID,避免多单去重互吞
            Long instanceId = record.getApprovalInstanceId();
            if (instanceId == null || instanceId == 5001L) {
                instanceId = record.getId();
            }
            req.setInstanceId(instanceId);
            req.setEnterpriseId(ent == null ? record.getEnterpriseId() : ent.getId());
            req.setEnterpriseName(ent == null ? null : ent.getEnterpriseName());
            req.setMatterName(matter == null ? null : matter.getMatterName());
            req.setApplicantName(ent == null ? null : ent.getEnterpriseName());
            req.setApprovalStatus(mapStatus(record.getStatus()));
            req.setSubmitTime(record.getSubmitTime());
            req.setFinishTime(record.getFinishTime());
            if (record.getSubmitTime() != null && record.getFinishTime() != null) {
                long hours = Duration.between(record.getSubmitTime(), record.getFinishTime()).toHours();
                req.setProcessDurationHours(BigDecimal.valueOf(Math.max(hours, 1)));
            }
            reportWithRetry(req, record.getId());
            log.info("申报 {} 已上报监管端, 状态={}, instanceId={}", record.getId(), record.getStatus(), instanceId);
        } catch (Exception e) {
            // 上报是增强行为,不中断申报主流程;但失败必须可见
            // (原 debug 级在生产等同静默,会造成"审批有动作、监管大屏不更新"且无迹可查)
            log.warn("申报上报监管端失败(已重试一次,监管大屏可能缺此条) recordId={} err={}", record.getId(), e.getMessage());
        }
    }

    /** 上报监管端:失败立即重试一次(应对对端重启/瞬时抖动);仍失败则抛出,由调用方记录 */
    private void reportWithRetry(SupervisionReportReq req, Long recordId) {
        try {
            supervisionClient.report(req);
        } catch (Exception first) {
            log.warn("申报上报监管端失败,立即重试一次 recordId={} err={}", recordId, first.getMessage());
            supervisionClient.report(req);
        }
    }

    /** 企业端状态 → 监管端口径 */
    private String mapStatus(String status) {
        if (status == null) {
            return "PENDING";
        }
        return switch (status) {
            case "PASSED", "APPROVED" -> "APPROVED";
            case "REJECTED" -> "REJECTED";
            default -> "PENDING"; // DRAFT/PENDING/APPROVING/REPAIR 统一按待审计
        };
    }
}
