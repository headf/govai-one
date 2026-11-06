package com.govai.supervision.controller;

import com.govai.supervision.common.PageResult;
import com.govai.supervision.common.Result;
import com.govai.supervision.annotation.RequirePermission;
import com.govai.supervision.dto.credit.CreditScoreReq;
import com.govai.supervision.entity.CreditEnterprise;
import com.govai.supervision.service.CreditEnterpriseService;
import com.govai.supervision.util.EnterpriseProfileClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * 企业信用评价控制器
 * 契约命名：/api/v1/credit/enterprise
 */
@Tag(name = "企业信用评价", description = "企业信用评分 / 等级")
@RestController
@RequestMapping("/api/v1/credit/enterprise")
@RequiredArgsConstructor
public class CreditEnterpriseController {

    private final CreditEnterpriseService creditEnterpriseService;
    private final EnterpriseProfileClient enterpriseProfileClient;

    /** 企业信用分页列表 */
    @Operation(summary = "企业信用分页列表")
    @GetMapping("/list")
    public Result<PageResult<CreditEnterprise>> list(@RequestParam(defaultValue = "1") long page,
                                                     @RequestParam(defaultValue = "10") long size,
                                                     @RequestParam(required = false) Long enterpriseId,
                                                     @RequestParam(required = false) String level) {
        return Result.success(creditEnterpriseService.pageEnterprise(page, size, enterpriseId, level));
    }

    /** 企业法人+经办人成员(跨端回填,弹窗展示) */
    @Operation(summary = "企业法人及成员")
    @GetMapping("/members")
    public Result<EnterpriseProfileClient.Members> members(@RequestParam Long enterpriseId) {
        return Result.success(enterpriseProfileClient.fetchMembers(enterpriseId));
    }

    /** 全量企业用户(含所属企业名,供用户管理"企业用户"Tab) */
    @Operation(summary = "全部企业用户")
    @GetMapping("/users/all-enterprise")
    public Result<List<EnterpriseProfileClient.EnterpriseUserItem>> allEnterpriseUsers() {
        return Result.success(enterpriseProfileClient.fetchAllEnterpriseUsers());
    }

    /** 监管处置:禁用企业经办人(不能禁法人账号) */
    @Operation(summary = "禁用企业经办人")
    @RequirePermission("user:edit")
    @PostMapping("/users/{id}/disable")
    public Result<Void> disableEnterpriseUser(@PathVariable Long id) {
        boolean ok = enterpriseProfileClient.disableEnterpriseUser(id);
        if (!ok) {
            return Result.fail(500, "禁用失败(企业端不可用或该账号不可禁)");
        }
        return Result.success();
    }

    /** 监管处置:启用企业经办人(解除禁用) */
    @Operation(summary = "启用企业经办人")
    @RequirePermission("user:edit")
    @PostMapping("/users/{id}/enable")
    public Result<Void> enableEnterpriseUser(@PathVariable Long id) {
        boolean ok = enterpriseProfileClient.enableEnterpriseUser(id);
        if (!ok) {
            return Result.fail(500, "启用失败(企业端不可用)");
        }
        return Result.success();
    }

    /** 查询某企业信用 */
    @Operation(summary = "企业信用详情")
    @GetMapping("/{enterpriseId}")
    public Result<CreditEnterprise> detail(@RequestParam Long enterpriseId) {
        return Result.success(creditEnterpriseService.getByEnterpriseId(enterpriseId));
    }

    /** 评分（新增/更新） */
    @Operation(summary = "企业评分/更新")
    @RequirePermission("credit:score")
    @PostMapping("/score")
    public Result<CreditEnterprise> score(@RequestBody @Valid CreditScoreReq req) {
        return Result.success("评分成功", creditEnterpriseService.saveOrUpdateScore(req));
    }

    /** 手动调整评分 */
    @Operation(summary = "手动调整评分")
    @RequirePermission("credit:score")
    @PutMapping("/adjust")
    public Result<BigDecimal> adjust(@RequestParam Long enterpriseId, @RequestParam BigDecimal score) {
        return Result.success("调整成功", creditEnterpriseService.adjustScore(enterpriseId, score));
    }

    /** 删除企业信用 */
    @Operation(summary = "删除企业信用")
    @RequirePermission("credit:score")
    @DeleteMapping("/{enterpriseId}")
    public Result<Void> delete(@RequestParam Long enterpriseId) {
        creditEnterpriseService.deleteByEnterpriseId(enterpriseId);
        return Result.success();
    }
}