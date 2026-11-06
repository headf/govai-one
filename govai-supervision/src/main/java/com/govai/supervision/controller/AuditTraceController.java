package com.govai.supervision.controller;

import com.govai.supervision.common.PageResult;
import com.govai.supervision.common.Result;
import com.govai.supervision.annotation.RequirePermission;
import com.govai.supervision.entity.AuditTrace;
import com.govai.supervision.service.AuditTraceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 审计追溯控制器
 * 契约命名：/api/v1/audit/traces
 */
@Tag(name = "审计追溯", description = "全链路审计 / 按实例回溯")
@RestController
@RequestMapping("/api/v1/audit/traces")
@RequiredArgsConstructor
public class AuditTraceController {

    private final AuditTraceService auditTraceService;

    /** 审计记录分页 */
    @Operation(summary = "审计记录分页")
    @GetMapping("/list")
    public Result<PageResult<AuditTrace>> list(@RequestParam(defaultValue = "1") long page,
                                               @RequestParam(defaultValue = "10") long size,
                                               @RequestParam(required = false) Long instanceId) {
        return Result.success(auditTraceService.pageTraces(page, size, instanceId));
    }

    /** 按实例回溯全链路（聚合审批/抽样/预警/审计） */
    @Operation(summary = "按实例号回溯全链路")
    @GetMapping("/chain/{instanceId}")
    public Result<String> chain(@PathVariable Long instanceId) {
        return Result.success(auditTraceService.traceChain(instanceId));
    }

    /** 手动写入一条审计（演示/补录场景） */
    @Operation(summary = "记录审计")
    @RequirePermission("audit:record")
    @PostMapping("/record")
    public Result<Void> record(@RequestParam Long instanceId, @RequestParam(required = false) String traceData) {
        auditTraceService.record(instanceId, traceData);
        return Result.success();
    }
}