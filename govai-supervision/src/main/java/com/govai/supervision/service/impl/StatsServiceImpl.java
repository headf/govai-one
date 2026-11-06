package com.govai.supervision.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.govai.supervision.entity.ApprovalDataCache;
import com.govai.supervision.entity.CreditEnterprise;
import com.govai.supervision.entity.RiskAlert;
import com.govai.supervision.mapper.ApprovalDataCacheMapper;
import com.govai.supervision.mapper.CreditEnterpriseMapper;
import com.govai.supervision.mapper.RiskAlertMapper;
import com.govai.supervision.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 统计大屏服务实现
 * 数据源：approval_data_cache（真实流程实时办件）+ risk_alert（预警）+ credit_enterprise（信用档案）。
 * 时序：纯按 approval_data_cache 真实办件按日聚合，无预置样例。
 */
@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final ApprovalDataCacheMapper approvalDataCacheMapper;
    private final RiskAlertMapper riskAlertMapper;
    private final CreditEnterpriseMapper creditEnterpriseMapper;

    /** 判定"办结"的审批状态 */
    private static final List<String> DONE_STATUS = List.of("APPROVED", "PASS");
    /** 超时阈值（小时） */
    private static final BigDecimal TIMEOUT_HOURS = new BigDecimal("48");

    @Override
    public Map<String, Object> overview() {
        Long total = approvalDataCacheMapper.selectCount(null);
        long totalCount = total == null ? 0 : total;

        // 办结数 = APPROVED + PASS
        long done = DONE_STATUS.stream()
                .mapToLong(s -> {
                    Long c = approvalDataCacheMapper.selectCount(
                            new LambdaQueryWrapper<ApprovalDataCache>().eq(ApprovalDataCache::getApprovalStatus, s));
                    return c == null ? 0 : c;
                })
                .sum();
        // 驳回数
        Long rejected = approvalDataCacheMapper.selectCount(
                new LambdaQueryWrapper<ApprovalDataCache>().eq(ApprovalDataCache::getApprovalStatus, "REJECTED"));
        long rejectedCount = rejected == null ? 0 : rejected;

        // 在办 = 处于 PENDING（或状态为空的在途件），不再用 total-done-rejected 减法（避免状态口径污染）
        Long pending = approvalDataCacheMapper.selectCount(
                new LambdaQueryWrapper<ApprovalDataCache>()
                        .and(w -> w.eq(ApprovalDataCache::getApprovalStatus, "PENDING")
                                .or().isNull(ApprovalDataCache::getApprovalStatus)));
        long pendingCount = pending == null ? 0 : pending;

        // 平均办理时长（仅统计办结有值的）
        List<ApprovalDataCache> all = approvalDataCacheMapper.selectList(
                new LambdaQueryWrapper<ApprovalDataCache>().isNotNull(ApprovalDataCache::getProcessDurationHours));
        double avgHours = all.stream()
                .mapToDouble(a -> a.getProcessDurationHours().doubleValue())
                .average().orElse(0);

        // 审批完成率 = 办结数 / 总数
        BigDecimal doneRate = totalCount == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(done)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalCount), 1, RoundingMode.HALF_UP);

        // 超时数（按超过 48h 近似）
        Long timeout = approvalDataCacheMapper.selectCount(
                new LambdaQueryWrapper<ApprovalDataCache>().gt(ApprovalDataCache::getProcessDurationHours, TIMEOUT_HOURS));
        long timeoutCount = timeout == null ? 0 : timeout;

        // 预警
        Long alertTotal = riskAlertMapper.selectCount(null);
        Long alertPending = riskAlertMapper.selectCount(
                new LambdaQueryWrapper<RiskAlert>().eq(RiskAlert::getHandleStatus, "PENDING"));

        Map<String, Object> map = new HashMap<>();
        map.put("totalApplications", totalCount);
        map.put("pendingCount", pendingCount);
        map.put("approvedCount", done);
        map.put("rejectedCount", rejectedCount);
        map.put("timeoutCount", timeoutCount);
        map.put("avgProcessHours", BigDecimal.valueOf(avgHours).setScale(2, RoundingMode.HALF_UP));
        map.put("doneRate", doneRate);                 // 办结率(%)
        map.put("alertTotal", alertTotal == null ? 0 : alertTotal);
        map.put("alertPending", alertPending == null ? 0 : alertPending);

        // 监管对象(企业信用档案)概况:总数 + 信用等级分布
        Long monitorTotal = creditEnterpriseMapper.selectCount(null);
        map.put("monitorEnterpriseCount", monitorTotal == null ? 0 : monitorTotal);
        List<CreditEnterprise> credits = creditEnterpriseMapper.selectList(null);
        Map<String, Long> levelCnt = new java.util.LinkedHashMap<>();
        for (String lv : new String[]{"A", "B", "C", "D"}) {
            levelCnt.put(lv, 0L);
        }
        for (CreditEnterprise c : credits) {
            String lv = c.getCreditLevel();
            if (lv != null && levelCnt.containsKey(lv)) {
                levelCnt.put(lv, levelCnt.get(lv) + 1);
            }
        }
        map.put("creditLevelDist", levelCnt);

        map.put("generateTime", LocalDate.now());
        return map;
    }

    @Override
    public List<Map<String, Object>> dailyTrend(LocalDate start, LocalDate end) {
        if (start == null) {
            start = LocalDate.now().minusDays(6);
        }
        if (end == null) {
            end = LocalDate.now();
        }
        // 真实办件按日聚合（无预置样例/无样例优先，杜绝假数据）
        List<ApprovalDataCache> records = approvalDataCacheMapper.selectList(
                new LambdaQueryWrapper<ApprovalDataCache>()
                        .isNotNull(ApprovalDataCache::getSubmitTime)
                        .between(ApprovalDataCache::getSubmitTime, start.atStartOfDay(), end.plusDays(1).atStartOfDay()));
        Map<LocalDate, List<ApprovalDataCache>> byDate = records.stream()
                .collect(Collectors.groupingBy(r -> r.getSubmitTime().toLocalDate()));

        List<Map<String, Object>> list = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            List<ApprovalDataCache> dayRecords = byDate.getOrDefault(d, List.of());
            long total = dayRecords.size();
            long approved = dayRecords.stream().filter(r -> DONE_STATUS.contains(r.getApprovalStatus())).count();
            long rejected = dayRecords.stream().filter(r -> "REJECTED".equals(r.getApprovalStatus())).count();
            long timeout = dayRecords.stream()
                    .filter(r -> r.getProcessDurationHours() != null
                            && r.getProcessDurationHours().compareTo(TIMEOUT_HOURS) > 0)
                    .count();
            double avg = dayRecords.stream()
                    .filter(r -> r.getProcessDurationHours() != null)
                    .mapToDouble(r -> r.getProcessDurationHours().doubleValue())
                    .average().orElse(0);
            Map<String, Object> item = new HashMap<>();
            item.put("statDate", d.toString());
            item.put("totalApplications", total);
            item.put("approvedCount", approved);
            item.put("rejectedCount", rejected);
            item.put("avgProcessHours", BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
            item.put("timeoutCount", timeout);
            list.add(item);
        }
        return list;
    }

    @Override
    public List<Map<String, Object>> hotMatters() {
        List<ApprovalDataCache> rows = approvalDataCacheMapper.selectList(
                new LambdaQueryWrapper<ApprovalDataCache>().isNotNull(ApprovalDataCache::getMatterName));
        return rows.stream()
                .collect(Collectors.groupingBy(ApprovalDataCache::getMatterName, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("matterName", e.getKey());
                    m.put("count", e.getValue());
                    return m;
                }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> topDuration(int size) {
        List<ApprovalDataCache> rows = approvalDataCacheMapper.selectList(
                new LambdaQueryWrapper<ApprovalDataCache>()
                        .isNotNull(ApprovalDataCache::getProcessDurationHours)
                        .isNotNull(ApprovalDataCache::getFinishTime)
                        .orderByDesc(ApprovalDataCache::getProcessDurationHours)
                        .last("limit " + Math.max(1, Math.min(size, 20))));
        return rows.stream().map(a -> {
            Map<String, Object> m = new HashMap<>();
            m.put("matterName", a.getMatterName());
            m.put("applicantName", a.getApplicantName());
            m.put("approvalStatus", a.getApprovalStatus());
            m.put("processDurationHours", a.getProcessDurationHours() == null ? null
                    : a.getProcessDurationHours().setScale(1, RoundingMode.HALF_UP).doubleValue());
            m.put("finishTime", a.getFinishTime() == null ? null : a.getFinishTime().toString());
            return m;
        }).collect(Collectors.toList());
    }
}
