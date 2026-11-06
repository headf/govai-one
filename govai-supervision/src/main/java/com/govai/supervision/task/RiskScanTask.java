package com.govai.supervision.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.govai.supervision.common.BizNoGenerator;
import com.govai.supervision.entity.ApprovalDataCache;
import com.govai.supervision.entity.RiskAlert;
import com.govai.supervision.entity.RiskRule;
import com.govai.supervision.mapper.ApprovalDataCacheMapper;
import com.govai.supervision.mapper.RiskAlertMapper;
import com.govai.supervision.mapper.RiskRuleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 风险规则匹配定时任务
 * 定期扫描审批数据缓存，命中的风险写入 risk_alert。
 * 三种规则：
 *  - TIMEOUT  ：processDurationHours 超过阈值 → 超期未办结告警（按实例）
 *  - THRESHOLD：近 windowDays 天内办件总量超过阈值 → 申请量突增告警（按系统/主体）
 *  - ANOMALY  ：同一主体近 windowDays 天内申请次数超过阈值 → 重复申报/异常告警
 * 去重策略：同一 (ruleId, targetType, targetId) 若存在未处理(PENDING)预警则跳过；
 *           已处理(HANDLED)后可再次触发（修复"处理后再复发"缺失）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RiskScanTask {

    private final RiskRuleMapper riskRuleMapper;
    private final ApprovalDataCacheMapper approvalDataCacheMapper;
    private final RiskAlertMapper riskAlertMapper;

    /** 默认统计窗口（天） */
    private static final int DEFAULT_WINDOW_DAYS = 7;

    @Scheduled(cron = "0 */5 * * * *")
    public void scanRisk() {
        List<RiskRule> rules = riskRuleMapper.selectList(
                new LambdaQueryWrapper<RiskRule>().eq(RiskRule::getStatus, 1));
        if (rules.isEmpty()) {
            return;
        }
        int created = 0;
        for (RiskRule rule : rules) {
            String type = rule.getRuleType();
            try {
                if ("TIMEOUT".equals(type)) {
                    created += scanTimeout(rule);
                } else if ("THRESHOLD".equals(type)) {
                    created += scanThreshold(rule);
                } else if ("ANOMALY".equals(type)) {
                    created += scanAnomaly(rule);
                }
            } catch (Exception e) {
                log.error("风险规则扫描异常 ruleId={}, type={}", rule.getId(), type, e);
            }
        }
        // 企业维度预警:使企业端 ent-view 预警页有数据(按企业近窗口审批动态生成,ENT-perp targetType=ENTERPRISE)
        try {
            created += scanEnterpriseAlerts();
        } catch (Exception e) {
            log.error("企业维度预警扫描异常", e);
        }
        if (created > 0) {
            log.info("风险扫描完成，本次新增预警 {} 条", created);
        }
    }

    /**
     * 企业维度预警:对近 windowDays 有审批记录的企业,按其审批动态生成 ENTERPRISE 级预警,
     * 供企业端信用页(/api/v1/ent-view/alerts, targetType=ENTERPRISE)消费。
     * 触发语义: 该企业近窗口存在未办结/定稿异常/被退回 的申报 → 提示关注。
     * 去重:同企业存在未处理 ENTERPRISE 预警则跳过,避免每次扫描刷屏。
     */
    private int scanEnterpriseAlerts() {
        Integer windowDays = DEFAULT_WINDOW_DAYS;
        LocalDateTime since = LocalDateTime.now().minusDays(windowDays);
        List<ApprovalDataCache> records = approvalDataCacheMapper.selectList(
                new LambdaQueryWrapper<ApprovalDataCache>()
                        .ge(ApprovalDataCache::getSubmitTime, since)
                        .isNotNull(ApprovalDataCache::getEnterpriseId));
        Map<Long, List<ApprovalDataCache>> byEnterprise = records.stream()
                .collect(Collectors.groupingBy(ApprovalDataCache::getEnterpriseId));
        int count = 0;
        for (Map.Entry<Long, List<ApprovalDataCache>> e : byEnterprise.entrySet()) {
            Long enterpriseId = e.getKey();
            List<ApprovalDataCache> recs = e.getValue();
            if (recs.isEmpty()) {
                continue;
            }
            long total = recs.size();
            long unfinished = recs.stream()
                    .filter(r -> "PENDING".equals(r.getApprovalStatus()) || "APPROVING".equals(r.getApprovalStatus()))
                    .count();
            // 已有未处理预警则跳过
            if (hasPendingEnterprise(enterpriseId)) {
                continue;
            }
            RiskAlert alert = new RiskAlert();
            alert.setAlertNo(BizNoGenerator.gen("YJ"));
            alert.setRuleId(null);
            alert.setTargetType("ENTERPRISE");
            alert.setTargetId(enterpriseId);
            alert.setAlertLevel(unfinished > 0 ? "MEDIUM" : "LOW");
            String statusDesc = unfinished > 0
                    ? "近 " + windowDays + " 天提交 " + total + " 笔申报，其中 " + unfinished + " 笔仍在办理中"
                    : "近 " + windowDays + " 天累计办理申报 " + total + " 笔";
            alert.setAlertContent("审批动态关注：" + statusDesc + "，请留意办理进度");
            alert.setAlertTime(LocalDateTime.now());
            alert.setHandleStatus("PENDING");
            riskAlertMapper.insert(alert);
            count++;
        }
        return count;
    }

    /** 该企业是否已有未处理 ENTERPRISE 预警 */
    private boolean hasPendingEnterprise(Long enterpriseId) {
        Long c = riskAlertMapper.selectCount(new LambdaQueryWrapper<RiskAlert>()
                .eq(RiskAlert::getTargetType, "ENTERPRISE")
                .eq(RiskAlert::getTargetId, enterpriseId)
                .eq(RiskAlert::getHandleStatus, "PENDING"));
        return c != null && c > 0;
    }

    /** TIMEOUT：办件处理时长超过阈值（小时） */
    private int scanTimeout(RiskRule rule) {
        BigDecimal threshold = toDecimal(rule.getThresholdValue());
        if (threshold == null) {
            return 0;
        }
        List<ApprovalDataCache> records = approvalDataCacheMapper.selectList(
                new LambdaQueryWrapper<ApprovalDataCache>()
                        .gt(ApprovalDataCache::getProcessDurationHours, threshold));
        int count = 0;
        for (ApprovalDataCache record : records) {
            Long instanceId = record.getInstanceId();
            if (instanceId == null || hasPending(rule.getId(), "INSTANCE", instanceId)) {
                continue;
            }
            RiskAlert alert = new RiskAlert();
            alert.setAlertNo(BizNoGenerator.gen("YJ"));
            alert.setRuleId(rule.getId());
            alert.setTargetType("INSTANCE");
            alert.setTargetId(instanceId);
            alert.setAlertLevel(rule.getAlertLevel());
            alert.setAlertContent("审批超时告警：办件【" + record.getMatterName() + "】处理时长 "
                    + record.getProcessDurationHours() + " 小时，超过阈值 " + rule.getThresholdValue() + " 小时");
            alert.setAlertTime(LocalDateTime.now());
            alert.setHandleStatus("PENDING");
            riskAlertMapper.insert(alert);
            count++;
        }
        return count;
    }

    /** THRESHOLD：窗口期办件总量超过阈值（近 windowDays 天，按日累计） */
    private int scanThreshold(RiskRule rule) {
        Integer windowDays = windowDaysOf(rule);
        LocalDateTime since = LocalDateTime.now().minusDays(windowDays);
        long total = countSubmitted(since);
        BigDecimal threshold = toDecimal(rule.getThresholdValue());
        if (threshold == null || threshold.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        if (BigDecimal.valueOf(total).compareTo(threshold) <= 0) {
            return 0;
        }
        // 该规则若已有未处理"系统级"突增预警则跳过（处理后可在下一扫描周期复发）
        if (hasPending(rule.getId(), "SYSTEM", null)) {
            return 0;
        }
        RiskAlert alert = new RiskAlert();
        alert.setAlertNo(BizNoGenerator.gen("YJ"));
        alert.setRuleId(rule.getId());
        alert.setTargetType("SYSTEM");
        alert.setAlertLevel(rule.getAlertLevel());
        alert.setAlertContent("申请量突增告警：近 " + windowDays + " 天提交办件 " + total
                + " 件，超过阈值 " + rule.getThresholdValue() + " 件");
        alert.setAlertTime(LocalDateTime.now());
        alert.setHandleStatus("PENDING");
        riskAlertMapper.insert(alert);
        return 1;
    }

    /** ANOMALY：同一申请人近 windowDays 天申请次数超过阈值 → 重复申报异常 */
    private int scanAnomaly(RiskRule rule) {
        Integer windowDays = windowDaysOf(rule);
        LocalDateTime since = LocalDateTime.now().minusDays(windowDays);
        int threshold = toInt(rule.getThresholdValue(), 5);
        // 按申请人聚合窗口期申报数
        List<ApprovalDataCache> records = approvalDataCacheMapper.selectList(
                new LambdaQueryWrapper<ApprovalDataCache>()
                        .ge(ApprovalDataCache::getSubmitTime, since)
                        .isNotNull(ApprovalDataCache::getApplicantName));
        Map<String, Long> grouped = records.stream()
                .collect(Collectors.groupingBy(ApprovalDataCache::getApplicantName, Collectors.counting()));
        int count = 0;
        for (Map.Entry<String, Long> e : grouped.entrySet()) {
            if (e.getValue() < threshold) {
                continue;
            }
            Long applicantKey = applicantKey(e.getKey());
            if (hasPending(rule.getId(), "APPLICANT", applicantKey)) {
                continue;
            }
            RiskAlert alert = new RiskAlert();
            alert.setAlertNo(BizNoGenerator.gen("YJ"));
            alert.setRuleId(rule.getId());
            alert.setTargetType("APPLICANT");
            alert.setTargetId(applicantKey);
            alert.setAlertLevel(rule.getAlertLevel());
            alert.setAlertContent("异常申报告警：申请人【" + e.getKey() + "】近 " + windowDays
                    + " 天提交 " + e.getValue() + " 次，疑似重复/异常申报");
            alert.setAlertTime(LocalDateTime.now());
            alert.setHandleStatus("PENDING");
            riskAlertMapper.insert(alert);
            count++;
        }
        return count;
    }

    private long countSubmitted(LocalDateTime since) {
        Long c = approvalDataCacheMapper.selectCount(
                new LambdaQueryWrapper<ApprovalDataCache>().ge(ApprovalDataCache::getSubmitTime, since));
        return c == null ? 0 : c;
    }

    /** 是否有未处理预警（同 rule + targetType + targetId，targetId 可空=系统级） */
    private boolean hasPending(Long ruleId, String targetType, Long targetId) {
        LambdaQueryWrapper<RiskAlert> wrapper = new LambdaQueryWrapper<RiskAlert>()
                .eq(RiskAlert::getRuleId, ruleId)
                .eq(RiskAlert::getTargetType, targetType)
                .eq(RiskAlert::getHandleStatus, "PENDING");
        if (targetId != null) {
            wrapper.eq(RiskAlert::getTargetId, targetId);
        }
        Long c = riskAlertMapper.selectCount(wrapper);
        return c != null && c > 0;
    }

    /** 窗口天数：threshold 存"天数"，空则默认 7 */
    private Integer windowDaysOf(RiskRule rule) {
        String extra = rule.getNotifyType();
        // 复用 notify_type 承载窗口天数扩展（如 "SITE:3"），保持兼容
        if (extra != null && extra.contains(":")) {
            try {
                return Integer.parseInt(extra.substring(extra.indexOf(':') + 1));
            } catch (NumberFormatException ignored) {
            }
        }
        return DEFAULT_WINDOW_DAYS;
    }

    /** 申请人名 → 稳定数值 key（告警 target_id 用） */
    private Long applicantKey(String name) {
        if (name == null) {
            return 0L;
        }
        long h = 1125899906842597L;
        for (int i = 0; i < name.length(); i++) {
            h = 31 * h + name.charAt(i);
        }
        return Math.abs(h);
    }

    private Integer toInt(String v, int def) {
        BigDecimal d = toDecimal(v);
        return d == null ? def : d.intValue();
    }

    private BigDecimal toDecimal(String v) {
        if (v == null || v.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(v.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
