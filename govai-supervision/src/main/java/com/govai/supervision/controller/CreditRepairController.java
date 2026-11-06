package com.govai.supervision.controller;

import com.govai.supervision.common.PageResult;
import com.govai.supervision.common.Result;
import com.govai.supervision.annotation.RequirePermission;
import com.govai.supervision.dto.credit.CreditRepairAuditReq;
import com.govai.supervision.dto.credit.CreditRepairReq;
import com.govai.supervision.entity.CreditRepair;
import com.govai.supervision.service.CreditRepairService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 信用修复控制器
 * 契约命名：/api/v1/credit/repair
 */
@Tag(name = "信用修复", description = "修复申请提交 / 审核")
@RestController
@RequestMapping("/api/v1/credit/repair")
@RequiredArgsConstructor
public class CreditRepairController {

    private final CreditRepairService creditRepairService;

    /** 提交修复申请 */
    @Operation(summary = "提交修复申请")
    @PostMapping("/apply")
    public Result<CreditRepair> apply(@RequestBody @Valid CreditRepairReq req) {
        return Result.success("申请提交成功", creditRepairService.applyCreditRepair(req));
    }

    /** 申请分页列表 */
    @Operation(summary = "申请分页列表")
    @GetMapping("/list")
    public Result<PageResult<CreditRepair>> list(@RequestParam(defaultValue = "1") long page,
                                                 @RequestParam(defaultValue = "10") long size,
                                                 @RequestParam(required = false) String status,
                                                 @RequestParam(required = false) String targetType) {
        return Result.success(creditRepairService.pageRepairs(page, size, status, targetType));
    }

    /** 审核（PASS/REJECT） */
    @Operation(summary = "审核申请")
    @RequirePermission("repair:audit")
    @PostMapping("/audit")
    public Result<CreditRepair> audit(@RequestBody @Valid CreditRepairAuditReq req) {
        return Result.success("审核完成", creditRepairService.audit(req));
    }
}