package com.govai.supervision.controller;

import com.govai.supervision.common.Result;
import com.govai.supervision.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 统计大屏控制器（契约：/api/v1/stats/overview、/api/v1/stats/daily）
 */
@Tag(name = "统计大屏", description = "实时汇总 / 时序趋势")
@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    /** 实时关键指标（办件量/办结/超时/满意率/预警，大屏 KPI 卡片） */
    @Operation(summary = "实时汇总")
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.success(statsService.overview());
    }

    /** 按时序取历史（折线/柱状图），默认近 7 天 */
    @Operation(summary = "时序趋势")
    @GetMapping("/daily")
    public Result<List<Map<String, Object>>> daily(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return Result.success(statsService.dailyTrend(start, end));
    }

    /** 热门事项排行（大屏） */
    @Operation(summary = "热门事项排行")
    @GetMapping("/hot-matters")
    public Result<List<Map<String, Object>>> hotMatters() {
        return Result.success(statsService.hotMatters());
    }

    /** 办结耗时最长 Top（大屏风险侧栏） */
    @Operation(summary = "办结耗时Top")
    @GetMapping("/top-duration")
    public Result<List<Map<String, Object>>> topDuration(@RequestParam(defaultValue = "8") int size) {
        return Result.success(statsService.topDuration(size));
    }
}