package com.govai.supervision.controller;

import com.govai.supervision.common.PageResult;
import com.govai.supervision.common.Result;
import com.govai.supervision.annotation.RequirePermission;
import com.govai.supervision.dto.credit.CreditScoreReq;
import com.govai.supervision.entity.CreditPersonal;
import com.govai.supervision.service.CreditPersonalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 个人信用评价控制器
 * 契约命名：/api/v1/credit/personal
 */
@Tag(name = "个人信用评价", description = "个人信用评分 / 等级")
@RestController
@RequestMapping("/api/v1/credit/personal")
@RequiredArgsConstructor
public class CreditPersonalController {

    private final CreditPersonalService creditPersonalService;

    /** 个人信用分页 */
    @Operation(summary = "个人信用分页列表")
    @GetMapping("/list")
    public Result<PageResult<CreditPersonal>> list(@RequestParam(defaultValue = "1") long page,
                                                   @RequestParam(defaultValue = "10") long size,
                                                   @RequestParam(required = false) Long userId,
                                                   @RequestParam(required = false) String level) {
        return Result.success(creditPersonalService.pagePersonal(page, size, userId, level));
    }

    /** 查询某用户信用 */
    @Operation(summary = "个人信用详情")
    @GetMapping("/{userId}")
    public Result<CreditPersonal> detail(@RequestParam Long userId) {
        return Result.success(creditPersonalService.getByUserId(userId));
    }

    /** 评分 */
    @Operation(summary = "个人评分/更新")
    @RequirePermission("credit:score")
    @PostMapping("/score")
    public Result<CreditPersonal> score(@RequestBody @Valid CreditScoreReq req) {
        return Result.success("评分成功", creditPersonalService.saveOrUpdateScore(req));
    }

    /** 删除 */
    @Operation(summary = "删除个人信用")
    @RequirePermission("credit:score")
    @DeleteMapping("/{userId}")
    public Result<Void> delete(@RequestParam Long userId) {
        creditPersonalService.deleteByUserId(userId);
        return Result.success();
    }
}