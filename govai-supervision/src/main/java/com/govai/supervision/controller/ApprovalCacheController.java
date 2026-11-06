package com.govai.supervision.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.govai.supervision.common.PageResult;
import com.govai.supervision.common.Result;
import com.govai.supervision.entity.ApprovalDataCache;
import com.govai.supervision.mapper.ApprovalDataCacheMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 审批数据（实时）控制器
 * 数据由真实流程自动流入：企业端申报 HTTP 上报 + 审批端 RabbitMQ 推送，
 * 由 ApprovalDataConsumer 落 approval_data_cache。本控制器只读展示，不做手工增删。
 */
@Tag(name = "审批数据", description = "真实流程办件数据查询（只读）")
@RestController
@RequestMapping("/api/v1/approval-cache")
@RequiredArgsConstructor
public class ApprovalCacheController {

    private final ApprovalDataCacheMapper approvalDataCacheMapper;

    /** 审批缓存分页查看 */
    @Operation(summary = "审批缓存分页")
    @GetMapping("/list")
    public Result<PageResult<ApprovalDataCache>> list(@RequestParam(defaultValue = "1") long page,
                                                      @RequestParam(defaultValue = "10") long size,
                                                      @RequestParam(required = false) String status) {
        LambdaQueryWrapper<ApprovalDataCache> wrapper = new LambdaQueryWrapper<ApprovalDataCache>()
                .eq(status != null && !status.isBlank(), ApprovalDataCache::getApprovalStatus, status)
                .orderByDesc(ApprovalDataCache::getReceiveTime);
        return Result.success(PageResult.of(approvalDataCacheMapper.selectPage(new Page<>(page, size), wrapper)));
    }
}
