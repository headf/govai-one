package com.govai.approval.controller;

import com.govai.approval.common.PageResult;
import com.govai.approval.common.Result;
import com.govai.approval.entity.AuditLog;
import com.govai.approval.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 审计日志 Controller
 *
 * 需求5.3：留痕数据不可修改，仅供审计查询
 * 支持按 事项实例 / 办理人 / 时间段 / 操作类型 四个维度筛选
 */
@Tag(name = "审计日志")
@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @Operation(summary = "审计日志分页查询")
    @GetMapping("/list")
    public Result<PageResult<AuditLog>> page(
            @RequestParam(required = false) Long instanceId,
            @RequestParam(required = false) Long operatorId,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(auditLogService.page(instanceId, operatorId, operationType, from, to, page, size));
    }
}
