package com.govai.supervision.controller;

import com.govai.supervision.common.Result;
import com.govai.supervision.annotation.RequirePermission;
import com.govai.supervision.dto.risk.RiskRuleDTO;
import com.govai.supervision.entity.RiskRule;
import com.govai.supervision.service.RiskRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 风险预警规则控制器
 * 契约命名：/api/v1/risk/rules
 */
@Tag(name = "风险预警规则", description = "预警规则 CRUD")
@RestController
@RequestMapping("/api/v1/risk/rules")
@RequiredArgsConstructor
public class RiskRuleController {

    private final RiskRuleService riskRuleService;

    /** 规则列表 */
    @Operation(summary = "规则列表")
    @GetMapping("/list")
    public Result<List<RiskRule>> list(@RequestParam(required = false) String ruleType,
                                       @RequestParam(required = false) Integer status) {
        return Result.success(riskRuleService.listRules(ruleType, status));
    }

    /** 新增规则 */
    @Operation(summary = "新增规则")
    @RequirePermission("risk:manage")
    @PostMapping
    public Result<RiskRule> create(@RequestBody @Valid RiskRuleDTO dto) {
        return Result.success("新增成功", riskRuleService.createRule(dto));
    }

    /** 更新规则 */
    @Operation(summary = "更新规则")
    @RequirePermission("risk:manage")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid RiskRuleDTO dto) {
        dto.setId(id);
        riskRuleService.updateRule(dto);
        return Result.success();
    }

    /** 删除规则 */
    @Operation(summary = "删除规则")
    @RequirePermission("risk:manage")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        riskRuleService.deleteRule(id);
        return Result.success();
    }
}