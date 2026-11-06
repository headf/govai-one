package com.govai.supervision.controller;

import com.govai.supervision.common.PageResult;
import com.govai.supervision.common.Result;
import com.govai.supervision.annotation.RequirePermission;
import com.govai.supervision.entity.RiskAlert;
import com.govai.supervision.interceptor.JwtInterceptor;
import com.govai.supervision.service.RiskAlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 风险预警记录控制器
 * 契约命名：/api/v1/risk/alerts
 */
@Tag(name = "风险预警记录", description = "预警查询 / 处置")
@RestController
@RequestMapping("/api/v1/risk/alerts")
@RequiredArgsConstructor
public class RiskAlertController {

    private final RiskAlertService riskAlertService;

    /** 预警分页列表 */
    @Operation(summary = "预警分页列表")
    @GetMapping("/list")
    public Result<PageResult<RiskAlert>> list(@RequestParam(defaultValue = "1") long page,
                                              @RequestParam(defaultValue = "10") long size,
                                              @RequestParam(required = false) String level,
                                              @RequestParam(required = false) String handleStatus,
                                              @RequestParam(required = false) String targetType) {
        return Result.success(riskAlertService.pageAlerts(page, size, level, handleStatus, targetType));
    }

    /** 预警统计（总数/未处理，大屏用） */
    @Operation(summary = "预警统计")
    @GetMapping("/stats")
    public Result<Map<String, Long>> stats() {
        Map<String, Long> map = new HashMap<>();
        map.put("total", riskAlertService.countTotal());
        map.put("pending", riskAlertService.countPending());
        return Result.success(map);
    }

    /** 处置预警（记录意见与处置人） */
    @Operation(summary = "处置预警")
    @RequirePermission("alert:handle")
    @PostMapping("/{id}/handle")
    public Result<Void> handle(@PathVariable Long id,
                               @RequestParam(required = false) String opinion,
                               HttpServletRequest request) {
        Object handlerAttr = request.getAttribute(JwtInterceptor.ATTR_USER_ID);
        Long handlerId = handlerAttr == null ? null : Long.valueOf(handlerAttr.toString());
        riskAlertService.handle(id, opinion, handlerId);
        return Result.success();
    }

    /** 预警转定向抽查（无感监管自动分派） */
    @Operation(summary = "预警转定向抽查")
    @RequirePermission("alert:handle")
    @PostMapping("/{id}/dispatch")
    public Result<Void> dispatch(@PathVariable Long id, HttpServletRequest request) {
        Object handlerAttr = request.getAttribute(JwtInterceptor.ATTR_USER_ID);
        Long handlerId = handlerAttr == null ? null : Long.valueOf(handlerAttr.toString());
        riskAlertService.dispatch(id, handlerId);
        return Result.success();
    }
}