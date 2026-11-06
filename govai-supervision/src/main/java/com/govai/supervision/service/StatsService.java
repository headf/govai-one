package com.govai.supervision.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 统计大屏服务
 */
public interface StatsService {

    /** 实时汇总：办件量/在办/办结/驳回/超时/平均时长/满意率/预警 等关键指标 */
    Map<String, Object> overview();

    /** 按天时序（大屏折线/柱状）：办件量 / 办结量 / 超时量 / 满意率 */
    List<Map<String, Object>> dailyTrend(LocalDate start, LocalDate end);

    /** 热门事项排行（大屏）：按事项聚合办件量 Top10 */
    List<Map<String, Object>> hotMatters();

    /** 办结耗时最长 Top（大屏）：反映风险/低效办件 */
    List<Map<String, Object>> topDuration(int size);
}