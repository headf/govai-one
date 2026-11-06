package com.govai.approval.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.govai.approval.common.PageResult;
import com.govai.approval.common.Result;
import com.govai.approval.entity.ApprovalProcessNode;
import com.govai.approval.entity.ApprovalProcessTemplate;
import com.govai.approval.mapper.ApprovalProcessNodeMapper;
import com.govai.approval.mapper.ApprovalProcessTemplateMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 流程模板 Controller
 *
 * 需求5.1：管理员在后台通过拖拽方式配置审批流程
 * 包括受理、初审、复审、决定、办结等环节
 * 每个环节可绑定办理人/角色、办理时限、流转条件
 */
@Tag(name = "流程模板")
@RestController
@RequestMapping("/api/v1/process-template")
@RequiredArgsConstructor
public class ProcessTemplateController {

    private final ApprovalProcessTemplateMapper templateMapper;
    private final ApprovalProcessNodeMapper nodeMapper;

    @Operation(summary = "流程模板分页")
    @GetMapping("/list")
    public Result<PageResult<ApprovalProcessTemplate>> page(
            @RequestParam(required = false) String matterType,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        LambdaQueryWrapper<ApprovalProcessTemplate> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(matterType)) w.eq(ApprovalProcessTemplate::getMatterType, matterType);
        if (status != null) w.eq(ApprovalProcessTemplate::getStatus, status);
        w.orderByDesc(ApprovalProcessTemplate::getCreateTime);
        Page<ApprovalProcessTemplate> p = templateMapper.selectPage(new Page<>(page, size), w);
        return Result.success(PageResult.of(p.getRecords(), p.getTotal(), page, size));
    }

    @Operation(summary = "查模板详情(含环节)")
    @GetMapping("/{id}")
    public Result<TemplateDetailVO> detail(@PathVariable Long id) {
        ApprovalProcessTemplate t = templateMapper.selectById(id);
        if (t == null) return Result.fail(404, "模板不存在");
        List<ApprovalProcessNode> nodes = nodeMapper.selectList(
                new LambdaQueryWrapper<ApprovalProcessNode>()
                        .eq(ApprovalProcessNode::getTemplateId, id)
                        .orderByAsc(ApprovalProcessNode::getSortOrder));
        return Result.success(new TemplateDetailVO(t, nodes));
    }

    @Operation(summary = "新增流程模板(含环节)")
    @PostMapping
    public Result<Long> save(@RequestBody TemplateDetailVO vo) {
        ApprovalProcessTemplate t = vo.getTemplate();
        if (t.getId() == null) {
            t.setCreateTime(LocalDateTime.now());
            t.setUpdateTime(LocalDateTime.now());
            templateMapper.insert(t);
        } else {
            t.setUpdateTime(LocalDateTime.now());
            templateMapper.updateById(t);
            // 删除旧环节
            nodeMapper.delete(new LambdaQueryWrapper<ApprovalProcessNode>()
                    .eq(ApprovalProcessNode::getTemplateId, t.getId()));
        }
        // 重新插入环节并串联 next_node_id
        List<ApprovalProcessNode> nodes = vo.getNodes();
        if (!CollectionUtils.isEmpty(nodes)) {
            for (int i = 0; i < nodes.size(); i++) {
                ApprovalProcessNode n = nodes.get(i);
                n.setId(null);
                n.setTemplateId(t.getId());
                n.setSortOrder(i + 1);
                nodeMapper.insert(n);
            }
            // 设置 nextNodeId
            for (int i = 0; i < nodes.size(); i++) {
                ApprovalProcessNode n = nodes.get(i);
                Long nextId = (i + 1 < nodes.size()) ? nodes.get(i + 1).getId() : null;
                n.setNextNodeId(nextId);
                nodeMapper.updateById(n);
            }
        }
        return Result.success(t.getId());
    }

    @Operation(summary = "启用/停用模板")
    @PostMapping("/{id}/status/{status}")
    public Result<Void> switchStatus(@PathVariable Long id, @PathVariable Integer status) {
        ApprovalProcessTemplate t = new ApprovalProcessTemplate();
        t.setId(id);
        t.setStatus(status);
        t.setUpdateTime(LocalDateTime.now());
        templateMapper.updateById(t);
        return Result.success();
    }

    /** 模板详情 VO */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class TemplateDetailVO {
        private ApprovalProcessTemplate template;
        private List<ApprovalProcessNode> nodes;
    }
}
