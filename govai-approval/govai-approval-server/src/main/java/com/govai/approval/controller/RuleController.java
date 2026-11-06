package com.govai.approval.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.govai.approval.common.PageResult;
import com.govai.approval.common.Result;
import com.govai.approval.dto.RuleResultVO;
import com.govai.approval.entity.ApprovalRuleItem;
import com.govai.approval.entity.ApprovalRuleSet;
import com.govai.approval.mapper.ApprovalRuleItemMapper;
import com.govai.approval.mapper.ApprovalRuleSetMapper;
import com.govai.approval.service.RuleEngineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 规则引擎 Controller
 *
 * 需求5.2：管理员在后台配置规则集和规则项
 * 规则类型：NUMBER 数值比较 / TEXT 文本匹配 / DATE 日期判断 / LICENSE 证照查询
 * 逻辑组合：AND（全过）/ OR（任一过）
 */
@Tag(name = "规则引擎")
@RestController
@RequestMapping("/api/v1/rule")
@RequiredArgsConstructor
public class RuleController {

    private final ApprovalRuleSetMapper ruleSetMapper;
    private final ApprovalRuleItemMapper ruleItemMapper;
    private final RuleEngineService ruleEngineService;

    // ============ 规则集 CRUD ============

    @Operation(summary = "规则集分页查询")
    @GetMapping("/set/list")
    public Result<PageResult<ApprovalRuleSet>> pageSets(
            @RequestParam(required = false) String matterType,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        LambdaQueryWrapper<ApprovalRuleSet> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(matterType)) w.eq(ApprovalRuleSet::getMatterType, matterType);
        if (status != null) w.eq(ApprovalRuleSet::getStatus, status);
        w.orderByDesc(ApprovalRuleSet::getCreateTime);
        Page<ApprovalRuleSet> p = ruleSetMapper.selectPage(new Page<>(page, size), w);
        return Result.success(PageResult.of(p.getRecords(), p.getTotal(), page, size));
    }

    @Operation(summary = "查规则集详情(含规则项)")
    @GetMapping("/set/{id}")
    public Result<RuleSetDetailVO> detail(@PathVariable Long id) {
        ApprovalRuleSet ruleSet = ruleSetMapper.selectById(id);
        if (ruleSet == null) return Result.fail(404, "规则集不存在");
        List<ApprovalRuleItem> items = ruleItemMapper.selectList(
                new LambdaQueryWrapper<ApprovalRuleItem>()
                        .eq(ApprovalRuleItem::getRuleSetId, id)
                        .orderByAsc(ApprovalRuleItem::getSortOrder));
        return Result.success(new RuleSetDetailVO(ruleSet, items));
    }

    @Operation(summary = "新增/修改规则集")
    @PostMapping("/set")
    public Result<Long> saveSet(@RequestBody RuleSetDetailVO vo) {
        ApprovalRuleSet set = vo.getRuleSet();
        if (set.getId() == null) {
            set.setCreateTime(LocalDateTime.now());
            ruleSetMapper.insert(set);
        } else {
            ruleSetMapper.updateById(set);
            // 删除旧规则项
            ruleItemMapper.delete(new LambdaQueryWrapper<ApprovalRuleItem>()
                    .eq(ApprovalRuleItem::getRuleSetId, set.getId()));
        }
        // 重新插入规则项
        List<ApprovalRuleItem> items = vo.getItems();
        if (items != null) {
            for (int i = 0; i < items.size(); i++) {
                ApprovalRuleItem item = items.get(i);
                item.setId(null);
                item.setRuleSetId(set.getId());
                item.setSortOrder(i + 1);
                ruleItemMapper.insert(item);
            }
        }
        return Result.success(set.getId());
    }

    @Operation(summary = "删除规则集")
    @DeleteMapping("/set/{id}")
    public Result<Void> deleteSet(@PathVariable Long id) {
        ruleSetMapper.deleteById(id);
        ruleItemMapper.delete(new LambdaQueryWrapper<ApprovalRuleItem>()
                .eq(ApprovalRuleItem::getRuleSetId, id));
        return Result.success();
    }

    @Operation(summary = "启用/停用规则集")
    @PostMapping("/set/{id}/status/{status}")
    public Result<Void> switchStatus(@PathVariable Long id, @PathVariable Integer status) {
        ApprovalRuleSet set = new ApprovalRuleSet();
        set.setId(id);
        set.setStatus(status);
        ruleSetMapper.updateById(set);
        return Result.success();
    }

    // ============ 规则测试 ============

    @Operation(summary = "规则引擎测试（按规则集ID执行）")
    @PostMapping("/set/{id}/test")
    public Result<RuleResultVO> testRule(@PathVariable Long id, @RequestBody Map<String, Object> formData) {
        return Result.success(ruleEngineService.checkByRuleSetId(id, formData));
    }

    // ============ VO ============
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class RuleSetDetailVO {
        private ApprovalRuleSet ruleSet;
        private List<ApprovalRuleItem> items;
    }
}
