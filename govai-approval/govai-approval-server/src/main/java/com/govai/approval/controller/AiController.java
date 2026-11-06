package com.govai.approval.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.govai.approval.common.PageResult;
import com.govai.approval.common.Result;
import com.govai.approval.dto.AiChatReq;
import com.govai.approval.dto.AiChatVO;
import com.govai.approval.dto.RuleResultVO;
import com.govai.approval.entity.AiPromptTemplate;
import com.govai.approval.entity.ApprovalProcessTemplate;
import com.govai.approval.mapper.ApprovalProcessTemplateMapper;
import com.govai.approval.service.AiAssistService;
import com.govai.approval.service.ApprovalProcessService;
import com.govai.approval.service.RuleEngineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI 能力 Controller
 *
 * 提供对外4个跨服务接口中的2个：
 *   接口6: POST /api/v1/ai/chat      大模型对话（群众端调用）
 *   接口7: POST /api/v1/ai/summary  数据摘要（监管端调用）
 *
 * 以及审批员工作台对内接口：
 *   审批意见生成 / 规则引擎测试 / Prompt 模板管理
 *
 * 设计约束：AI 输出仅为辅助参考，不直接决定审批结果
 */
@Tag(name = "AI能力")
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiAssistService aiAssistService;
    private final RuleEngineService ruleEngineService;
    private final ApprovalProcessService processService;
    private final ApprovalProcessTemplateMapper templateMapper;

    @Operation(summary = "接口6-大模型对话（群众端调用）")
    @PostMapping("/chat")
    public Result<AiChatVO> chat(@Valid @RequestBody AiChatReq req) {
        return Result.success(aiAssistService.chat(req));
    }

    @Operation(summary = "接口7-数据摘要（监管端调用）")
    @PostMapping("/summary")
    public Result<AiChatVO> summary(@RequestBody Map<String, String> body) {
        String content = body == null ? "" : body.getOrDefault("content", "");
        return Result.success(aiAssistService.summary(content));
    }

    @Operation(summary = "生成审批意见（AI辅助研判）")
    @PostMapping("/opinion/{instanceId}")
    public Result<AiChatVO> generateOpinion(@PathVariable Long instanceId) {
        // 先跑规则引擎，把规则结果作为AI输入
        com.govai.approval.entity.ApprovalInstance inst = processService.getById(instanceId);
        Map<String, Object> formData = parseFormData(inst == null ? null : inst.getFormData());
        // 通过模板获取事项类型编码（规则集按 matterType 匹配）
        String matterType = null;
        if (inst != null && inst.getTemplateId() != null) {
            ApprovalProcessTemplate tpl = templateMapper.selectById(inst.getTemplateId());
            if (tpl != null) matterType = tpl.getMatterType();
        }
        RuleResultVO ruleResult = ruleEngineService.checkByMatterType(matterType, formData);
        return Result.success(aiAssistService.generateApprovalOpinion(instanceId, ruleResult));
    }

    @Operation(summary = "查询实例最近一次已生成的AI审批意见(提交时后台预生成,详情页直接读取)")
    @GetMapping("/opinion/latest/{instanceId}")
    public Result<com.govai.approval.entity.AiChatRecord> latestOpinion(@PathVariable Long instanceId) {
        return Result.success(aiAssistService.findLatestOpinion(instanceId));
    }

    @Operation(summary = "Prompt模板分页")
    @GetMapping("/prompt/list")
    public Result<PageResult<AiPromptTemplate>> pageTemplates(
            @RequestParam(required = false) String sceneType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(aiAssistService.pageTemplates(sceneType, page, size));
    }

    @Operation(summary = "新增/修改Prompt模板")
    @PostMapping("/prompt")
    public Result<AiPromptTemplate> saveTemplate(@RequestBody AiPromptTemplate tpl) {
        return Result.success(aiAssistService.saveTemplate(tpl));
    }

    @Operation(summary = "删除Prompt模板")
    @DeleteMapping("/prompt/{id}")
    public Result<Void> deleteTemplate(@PathVariable Long id) {
        aiAssistService.deleteTemplate(id);
        return Result.success();
    }

    @Operation(summary = "规则引擎测试（按事项类型执行）")
    @PostMapping("/rule/check/{matterType}")
    public Result<RuleResultVO> ruleCheck(@PathVariable String matterType,
                                          @RequestBody Map<String, Object> formData) {
        return Result.success(ruleEngineService.checkByMatterType(matterType, formData));
    }

    @Operation(summary = "按审批实例运行规则审核")
    @PostMapping("/rule/check-by-instance/{instanceId}")
    public Result<RuleResultVO> ruleCheckByInstance(@PathVariable Long instanceId) {
        com.govai.approval.entity.ApprovalInstance inst = processService.getById(instanceId);
        if (inst == null) return Result.fail(404, "审批实例不存在");
        Map<String, Object> formData = parseFormData(inst.getFormData());
        String matterType = null;
        if (inst.getTemplateId() != null) {
            ApprovalProcessTemplate tpl = templateMapper.selectById(inst.getTemplateId());
            if (tpl != null) matterType = tpl.getMatterType();
        }
        return Result.success(ruleEngineService.checkByMatterType(matterType, formData));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseFormData(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, Map.class);
        } catch (Exception e) {
            return null;
        }
    }
}
