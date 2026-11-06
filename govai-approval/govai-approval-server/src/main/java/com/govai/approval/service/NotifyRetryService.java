package com.govai.approval.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.govai.approval.entity.AuditLog;
import com.govai.approval.feign.CitizenClient;
import com.govai.approval.feign.EnterpriseClient;
import com.govai.approval.mapper.AuditLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 审批终态回调重试（审批端 → 企业端 / 群众端）。
 *
 * 修复的漏洞：回调失败原来只记日志，会造成"审批已办结，但企业端/群众端状态一直不更新"的跨端不一致。
 * 做法：失败时把回调参数写入 audit_log(type=NOTIFY_FAIL)，由本服务定时重试；
 *       重试成功改记为 NOTIFY_OK；连续失败达上限改记为 NOTIFY_GIVEUP（留痕待人工介入）。
 * 不引入新表，复用审计日志作为重试凭据，重启不丢。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyRetryService {

    private static final String TYPE_FAIL = "NOTIFY_FAIL";
    private static final String TYPE_OK = "NOTIFY_OK";
    private static final String TYPE_GIVEUP = "NOTIFY_GIVEUP";
    private static final int MAX_RETRY = 5;
    private static final String TARGET_ENT = "ENT";

    private final AuditLogMapper auditLogMapper;
    private final EnterpriseClient enterpriseClient;
    private final CitizenClient citizenClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 回调失败时登记待重试（不阻断审批主流程） */
    public void recordFailure(String target, Long instanceId, String status, String opinion, String error) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("target", target);        // ENT=企业端 / CIT=群众端
            payload.put("instanceId", instanceId);
            payload.put("status", status);
            payload.put("opinion", opinion);
            payload.put("retry", 0);
            payload.put("error", error);

            AuditLog alog = new AuditLog();
            alog.setInstanceId(instanceId);
            alog.setOperatorName("系统");
            alog.setOperationType(TYPE_FAIL);
            alog.setOperationOpinion(toJson(payload));
            alog.setOperationTime(LocalDateTime.now());
            auditLogMapper.insert(alog);
        } catch (Exception e) {
            log.warn("登记回调重试时出错(忽略) instanceId={} err={}", instanceId, e.getMessage());
        }
    }

    /** 每 60s 重试一次未成功的回调（最多 20 条/轮，避免长时间占用） */
    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    @SuppressWarnings("unchecked")
    public void retryFailedNotifications() {
        List<AuditLog> fails = auditLogMapper.selectList(new LambdaQueryWrapper<AuditLog>()
                .eq(AuditLog::getOperationType, TYPE_FAIL)
                .orderByAsc(AuditLog::getOperationTime)
                .last("LIMIT 20"));
        if (fails.isEmpty()) {
            return;
        }
        for (AuditLog a : fails) {
            Map<String, Object> p;
            try {
                p = objectMapper.readValue(a.getOperationOpinion(), Map.class);
            } catch (Exception e) {
                giveUp(a, "报文解析失败: " + a.getOperationOpinion());
                continue;
            }
            String target = String.valueOf(p.get("target"));
            Long instanceId = p.get("instanceId") == null ? null : Long.valueOf(String.valueOf(p.get("instanceId")));
            String status = p.get("status") == null ? null : String.valueOf(p.get("status"));
            String opinion = p.get("opinion") == null ? null : String.valueOf(p.get("opinion"));
            int retry = p.get("retry") == null ? 0 : Integer.parseInt(String.valueOf(p.get("retry")));

            boolean ok = false;
            try {
                if (TARGET_ENT.equals(target)) {
                    enterpriseClient.notifyApprovalResult(instanceId, status, opinion);
                } else {
                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put("instanceId", instanceId);
                    body.put("status", status);
                    body.put("opinion", opinion);
                    citizenClient.notifyCitizenStatus(body);
                }
                ok = true;
            } catch (Exception e) {
                log.warn("回调重试失败 target={} instanceId={} retry={} err={}", target, instanceId, retry, e.getMessage());
            }

            if (ok) {
                a.setOperationType(TYPE_OK);
                a.setOperationOpinion(toJson(p));
                auditLogMapper.updateById(a);
                log.info("回调重试成功 target={} instanceId={}", target, instanceId);
            } else if (retry + 1 >= MAX_RETRY) {
                giveUp(a, toJson(p));
                log.error("回调重试超过 {} 次,放弃 target={} instanceId={} —— 需人工介入", MAX_RETRY, target, instanceId);
            } else {
                p.put("retry", retry + 1);
                a.setOperationOpinion(toJson(p));
                auditLogMapper.updateById(a);
            }
        }
    }

    private void giveUp(AuditLog a, String detail) {
        a.setOperationType(TYPE_GIVEUP);
        a.setOperationOpinion(detail);
        auditLogMapper.updateById(a);
    }

    private String toJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            return String.valueOf(o);
        }
    }
}
