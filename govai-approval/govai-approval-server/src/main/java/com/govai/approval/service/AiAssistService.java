package com.govai.approval.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.govai.approval.common.BusinessException;
import com.govai.approval.common.Constants;
import com.govai.approval.common.PageResult;
import com.govai.approval.config.AiProperties;
import com.govai.approval.dto.*;
import com.govai.approval.entity.*;
import com.govai.approval.mapper.*;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * AI 辅助 Service
 *
 * 三大场景：
 * 1. 政策问答（接口6：POST /api/v1/ai/chat）—— 群众端消费
 * 2. 审批意见生成（场景：OPINION）—— 审批员工作台调用，输出风险点+合规判断+建议决定
 * 3. 数据摘要（接口7：POST /api/v1/ai/summary）—— 监管端消费
 *
 * 设计约束：AI 输出仅为辅助参考，不直接决定审批结果，最终决定权归工作人员
 * 系统在审计日志中区分 AI建议 与 人工决定，确保可追溯
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiAssistService extends ServiceImpl<AiChatRecordMapper, AiChatRecord> {

    @Autowired(required = false)
    private OpenAiChatModel openAiChatModel;
    private final AiProperties aiProperties;

    /**
     * 判断 AI 模型是否可用（api-key 为占位符时不可用）
     */
    private boolean isModelAvailable() {
        if (openAiChatModel == null) return false;
        String key = aiProperties.getApiKey();
        return key != null && !key.isBlank() && !key.contains("placeholder") && !key.contains("your-");
    }
    private final AiPromptTemplateMapper promptTemplateMapper;
    private final com.govai.approval.feign.EnterpriseClient enterpriseClient;
    private final ApprovalInstanceMapper instanceMapper;
    private final AuditLogMapper auditLogMapper;

    /** 群众端政务问答默认风格: 简洁分点, 避免大段长文(不影响审批意见/摘要等显式 prompt 调用) */
    private static final String CUSTOMER_CHAT_PROMPT =
            "你是智慧政务办事助手，面向普通群众解答办事问题。"
            + "务必做到：回答简洁、口语化、直接给结论；正文控制在200字以内；"
            + "优先用 1. 2. 3. 这样 3~5 个要点分点说明，不要输出 Markdown 符号(#、**、*)；"
            + "结尾用一句话提示可追问的问题。";

    /**
     * 接口6：大模型对话
     */
    public AiChatVO chat(AiChatReq req) {
        long start = System.currentTimeMillis();
        // 1. 解析 Prompt 模板（若指定 templateId）
        String systemPrompt = req.getSystemPrompt();
        if (!StringUtils.hasText(systemPrompt) && req.getTemplateId() == null) {
            // 群众端问答未指定风格时, 默认按简洁分点回答, 避免内容过长过乱
            systemPrompt = CUSTOMER_CHAT_PROMPT;
        }
        String userMessage = req.getMessage();
        if (req.getTemplateId() != null) {
            AiPromptTemplate tpl = promptTemplateMapper.selectById(req.getTemplateId());
            if (tpl != null) {
                if (StringUtils.hasText(tpl.getSystemPrompt())) systemPrompt = tpl.getSystemPrompt();
                if (StringUtils.hasText(tpl.getUserPromptTemplate())) {
                    userMessage = renderTemplate(tpl.getUserPromptTemplate(), buildVariables(req));
                }
            }
        }
        // 2. 调用大模型
        String response = invokeModel(systemPrompt, userMessage);
        long cost = System.currentTimeMillis() - start;
        int tokenCount = estimateToken(userMessage + response);

        // 3. 保存对话记录
        AiChatRecord record = new AiChatRecord();
        record.setInstanceId(req.getInstanceId());
        record.setTemplateId(req.getTemplateId());
        record.setUserMessage(req.getMessage());
        record.setAiResponse(response);
        record.setTokenCount(tokenCount);
        record.setResponseTimeMs(cost);
        save(record);

        // 4. 若关联审批实例，写 AI_ADVICE 审计日志（与人工决定区分）
        if (req.getInstanceId() != null) {
            AuditLog log = new AuditLog();
            log.setInstanceId(req.getInstanceId());
            log.setOperatorId(null);  // AI 操作
            log.setOperatorName("AI助手");
            log.setOperationType(Constants.OP_AI_ADVICE);
            log.setOperationOpinion(truncate(response, 400));
            log.setAfterStatus(null);
            log.setOperationTime(LocalDateTime.now());
            auditLogMapper.insert(log);
        }

        return new AiChatVO(response, tokenCount, cost);
    }

    /**
     * 接口7：数据摘要（监管端调用）
     */
    public AiChatVO summary(String content) {
        AiChatReq req = new AiChatReq();
        req.setMessage(content);
        req.setSystemPrompt("你是政务数据摘要AI，将审批数据归纳为简明摘要，输出要点列表与风险提示。");
        return chat(req);
    }

    /**
     * 审批辅助：基于实例生成审核意见
     * 输出固定简体骨架（申报事项/申请人/规则校验结论/建议决定/AI核查要点），
     * "通过/未通过/是否办结"一律由规则引擎结果驱动，避免大模型意见与最终结果自相矛盾；
     * 模型正文经 normalizeOpinion 清洗（去 markdown/emoji/表格等符号）后只作"核查要点"保留。
     */
    public AiChatVO generateApprovalOpinion(Long instanceId, RuleResultVO ruleResult) {
        ApprovalInstance instance = instanceMapper.selectById(instanceId);
        if (instance == null) throw new BusinessException(404, "审批实例不存在");

        // 1) 规则结论与建议（驱动骨架，不交给大模型自由发挥）
        String ruleLabel = ruleLabelOf(ruleResult);
        String decision = decisionTextOf(ruleResult);

        // 2) 给模型的真实事实（逐项规则 + 表单原文，杜绝编造申报外细节）
        StringBuilder facts = new StringBuilder();
        if (ruleResult != null) {
            if (ruleResult.getItems() != null) {
                for (RuleResultVO.RuleItemResult it : ruleResult.getItems()) {
                    facts.append("- ").append(blank(it.getFieldName())).append("（").append(blank(it.getRuleType()))
                            .append(" ").append(blank(it.getOperator())).append(" ").append(blank(it.getCompareValue()))
                            .append("）: ").append(it.isPassed() ? "通过" : "未通过");
                    if (!it.isPassed() && StringUtils.hasText(it.getErrorMessage())) {
                        facts.append("，提示：").append(it.getErrorMessage());
                    }
                    facts.append("\n");
                }
            }
            if (ruleResult.getErrors() != null) {
                for (String e : ruleResult.getErrors()) {
                    if (StringUtils.hasText(e)) facts.append("未通过原因：").append(e).append("\n");
                }
            }
        }
        // 2.2) 申报材料清单（含上传文件名）：让 AI 能提示"材料与申报事项是否相符 / 有无明显异常"
        //      说明：无 OCR 时 AI 看不到材料内容，但可从"材料名 vs 上传文件名 vs 文件类型"判断是否可疑
        try {
            var mr = enterpriseClient.materialsByInstance(instanceId);
            java.util.List<Map<String, Object>> mats = (mr == null || mr.getData() == null)
                    ? new java.util.ArrayList<>() : mr.getData();
            if (mats.isEmpty()) {
                facts.append("申报材料：无上传材料（可能全部由电子证照免证办自动核验）\n");
            } else {
                facts.append("申报材料（材料名 / 上传文件名 / 文件类型）:\n");
                for (Map<String, Object> m : mats) {
                    facts.append("- ").append(blank(String.valueOf(m.get("materialName"))))
                            .append(" / ").append(blank(String.valueOf(m.get("originalName"))))
                            .append(" / ").append(blank(String.valueOf(m.get("fileType")))).append("\n");
                }
            }
        } catch (Exception e) {
            log.warn("取申报材料失败(忽略) instanceId={} err={}", instanceId, e.getMessage());
        }
        // 2.1) OPINION 模板仍可微调"AI 核查要点"的提问口径；结论与骨架始终由规则引擎驱动
        AiPromptTemplate opinionTpl = promptTemplateMapper.selectOne(
                new LambdaQueryWrapper<AiPromptTemplate>()
                        .eq(AiPromptTemplate::getSceneType, "OPINION")
                        .eq(AiPromptTemplate::getStatus, 1)
                        .last("LIMIT 1"));
        String factsText = facts.length() == 0 ? "未执行规则校验" : facts.toString();
        Map<String, Object> vars = new HashMap<>();
        vars.put("matterName", instance.getMatterName());
        vars.put("applicantName", instance.getApplicantName());
        vars.put("formData", truncate(instance.getFormData(), 600));
        vars.put("ruleConclusion", ruleLabel);
        vars.put("decision", decision);
        vars.put("facts", factsText);
        vars.put("ruleResult", factsText);
        String userPrompt;
        if (opinionTpl != null && StringUtils.hasText(opinionTpl.getUserPromptTemplate())) {
            userPrompt = renderTemplate(opinionTpl.getUserPromptTemplate(), vars);
        } else {
            userPrompt = "申报事项：" + instance.getMatterName()
                    + "\n申请人：" + instance.getApplicantName()
                    + "\n申报表单：" + truncate(instance.getFormData(), 600)
                    + "\n规则引擎逐项校验结果：\n" + factsText
                    + "\n请输出简要核查要点与风险提示。只输出要点正文，不要输出通过/不通过/建议决定等结论，也不要重复申报事项、申请人；"
                    + "不得编造申报表单中没有的信息，无法确认的事项写\"按现有材料无法进一步判断\"。";
        }
        String systemPrompt = "你是政务审批辅助AI。输出必须为纯文本，禁止使用任何 Markdown 符号（#、**、*、`、|表格、---）、emoji 或特殊符号；"
                + "核查要点与风险提示逐条另起一行、每条一句话且不超过25字，不要套话与总括性铺垫，不要重复；"
                + "如申报材料的名称与其上传文件名明显不符（例：要求\"从业人员健康证\"，实际文件名是\"简历.pdf\"）、或文件类型异常，必须在要点中直接指出并建议人工核实；"
                + "仅能引用上述申报表单、规则校验结果与申报材料中真实存在的信息，不得虚构或联想；"
                + "最终通过/不通过/是否办结由系统规则与人工决定，你只负责风险与核查提示，不输出结论或建议决定。";

        long start = System.currentTimeMillis();
        String raw = invokeModel(systemPrompt, userPrompt);
        long cost = System.currentTimeMillis() - start;
        String risk = normalizeOpinion(raw);
        if (!StringUtils.hasText(risk)) risk = "无";

        // 3) 固定骨架拼装
        String opinion = "申报事项：" + instance.getMatterName()
                + "\n申请人：" + instance.getApplicantName()
                + "\n规则校验结论：" + ruleLabel
                + "\n建议决定：" + decision
                + "\nAI 核查要点：" + risk;

        int tokenCount = estimateToken(userPrompt + opinion);

        // 4) 落库 + AI 建议留痕
        AiChatRecord record = new AiChatRecord();
        record.setInstanceId(instanceId);
        record.setUserMessage(userPrompt);
        record.setAiResponse(opinion);
        record.setTokenCount(tokenCount);
        record.setResponseTimeMs(cost);
        record.setCreateTime(LocalDateTime.now());
        save(record);

        AuditLog alog = new AuditLog();
        alog.setInstanceId(instanceId);
        alog.setOperatorId(null);  // AI 操作
        alog.setOperatorName("AI助手");
        alog.setOperationType(Constants.OP_AI_ADVICE);
        alog.setOperationOpinion(truncate(opinion, 400));
        alog.setAfterStatus(null);
        alog.setOperationTime(LocalDateTime.now());
        auditLogMapper.insert(alog);

        log.info("AI审核意见已生成 instanceId={}", instanceId);
        return new AiChatVO(opinion, tokenCount, cost);
    }

    /** 规则结果 → 中文结论（骨架首行，原样来自规则引擎，不交给模型发挥） */
    private String ruleLabelOf(RuleResultVO r) {
        if (r == null || !StringUtils.hasText(r.getResult())) return "未执行（规则集未配置或表单缺失）";
        return switch (r.getResult()) {
            case Constants.RULE_PASS -> "通过（规则项全部校验通过）";
            case Constants.RULE_FAIL -> "未通过（存在未通过的规则项）";
            case Constants.RULE_TO_MANUAL -> "转人工（未配置规则或需人工判定）";
            default -> r.getResult();
        };
    }

    /** 规则结果 → 建议决定（与直通/人工流转语义保持一致） */
    private String decisionTextOf(RuleResultVO r) {
        if (r == null) return "建议按正常流程转人工受理审核";
        return switch (r.getResult()) {
            case Constants.RULE_PASS -> "建议由办结（出证）岗对申报材料复核后办结（规则已全过，最终办结权在人工）";
            case Constants.RULE_FAIL -> "建议转受理/初审岗人工复核后决定（规则存在未通过项，不直达办结）";
            case Constants.RULE_TO_MANUAL -> "建议按正常流程转人工受理审核（规则集未配置或需人工判定）";
            default -> "建议转人工复核后决定";
        };
    }

    /**
     * 清洗大模型输出：去掉 Markdown/表格/水平线/emoji/杂项符号并折叠空行，
     * 保证 AI 意见呈现为干净的简体纯文本。
     */
    private String normalizeOpinion(String text) {
        if (text == null) return "";
        String s = text;
        // 模型可能输出字面 "\n",先统一成真换行再清洗
        s = s.replace("\\n", "\n");
        // 标题符、表格线、水平线
        s = s.replaceAll("(?m)^[#]{1,6}\\s*", "");
        s = s.replaceAll("(?m)^.*\\|.*$", "");
        s = s.replaceAll("(?m)^[=\\-*_]{3,}\\s*$", "");
        // 加粗/斜体/行内码/引用符
        s = s.replaceAll("\\*\\*|__|`|>\\s*", "");
        // emoji 与杂项符号（含 dingbat、区块符号、变体选择符、零宽连接）
        s = s.replaceAll("[\\u1F000-\\u1FAFF\\u2600-\\u27BF\\u2B00-\\u2BFF\\uFE0F\\u200D\\uE000-\\uF8FF]", "");
        // 单行内多余空白
        s = s.replaceAll("[ \\t]+", " ");
        // 折叠空行
        s = s.replaceAll("(?m)^[ \\t]*\\n", "\n").replaceAll("\\n{3,}", "\n\n");
        // 逐行 trim、去空行
        StringBuilder sb = new StringBuilder();
        for (String line : s.split("\n")) {
            String t = line.trim();
            if (t.isEmpty()) continue;
            sb.append(t).append('\n');
        }
        return sb.toString().trim();
    }

    private String blank(String s) {
        return StringUtils.hasText(s) ? s : "-";
    }

    /**
     * 查询某实例最近一次已生成的 AI 审批意见(提交后后台预生成的结果，审批人打开详情直接读取，无需等待大模型现算)。
     * 无则返回 null(前端保留手动生成入口)。
     */
    public AiChatRecord findLatestOpinion(Long instanceId) {
        if (instanceId == null) return null;
        return getOne(new LambdaQueryWrapper<AiChatRecord>()
                .eq(AiChatRecord::getInstanceId, instanceId)
                .orderByDesc(AiChatRecord::getId)
                .last("LIMIT 1"));
    }

    // =========== Prompt 模板管理 ===========
    public PageResult<AiPromptTemplate> pageTemplates(String sceneType, int page, int size) {
        LambdaQueryWrapper<AiPromptTemplate> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(sceneType)) w.eq(AiPromptTemplate::getSceneType, sceneType);
        w.orderByDesc(AiPromptTemplate::getCreateTime);
        Page<AiPromptTemplate> p = promptTemplateMapper.selectPage(new Page<>(page, size), w);
        return PageResult.of(p.getRecords(), p.getTotal(), page, size);
    }

    public AiPromptTemplate saveTemplate(AiPromptTemplate tpl) {
        if (tpl.getId() == null) {
            tpl.setCreateTime(LocalDateTime.now());
            promptTemplateMapper.insert(tpl);
        } else {
            promptTemplateMapper.updateById(tpl);
        }
        return tpl;
    }

    public void deleteTemplate(Long id) {
        promptTemplateMapper.deleteById(id);
    }

    // =========== 内部工具 ===========

    private String invokeModel(String systemPrompt, String userMessage) {
        if (!isModelAvailable()) {
            return mockResponse(systemPrompt, userMessage);
        }
        try {
            String prompt = (StringUtils.hasText(systemPrompt) ? systemPrompt + "\n\n" : "") + userMessage;
            return openAiChatModel.generate(prompt);
        } catch (Exception e) {
            log.error("调用大模型失败，降级返回 Mock。err={}", e.getMessage());
            return mockResponse(systemPrompt, userMessage);
        }
    }

    /**
     * Mock 响应：api-key 未配置时降级使用
     * 注意：仅用于开发联调，生产环境必须配置真实 api-key
     */
    private String mockResponse(String systemPrompt, String userMessage) {
        return """
                [Mock 响应 - 未配置真实 api-key]
                系统: %s
                用户: %s

                风险点提示: 无明显风险（Mock）
                合规性判断: 规则审核已通过（Mock）
                建议决定: 通过（仅供参考，最终决定权归工作人员）
                """.formatted(
                StringUtils.hasText(systemPrompt) ? truncate(systemPrompt, 50) : "(无)",
                truncate(userMessage, 200));
    }

    private Map<String, Object> buildVariables(AiChatReq req) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("message", req.getMessage());
        if (req.getInstanceId() != null) {
            ApprovalInstance inst = instanceMapper.selectById(req.getInstanceId());
            if (inst != null) {
                vars.put("matterName", inst.getMatterName());
                vars.put("applicantName", inst.getApplicantName());
                vars.put("formData", truncate(inst.getFormData(), 400));
            }
        }
        return vars;
    }

    /**
     * 简易模板渲染：替换 {varName} 占位
     */
    private String renderTemplate(String template, Map<String, Object> vars) {
        if (!StringUtils.hasText(template)) return "";
        String result = template;
        for (Map.Entry<String, Object> e : vars.entrySet()) {
            result = result.replace("{" + e.getKey() + "}", String.valueOf(e.getValue()));
        }
        return result;
    }

    private int estimateToken(String text) {
        if (!StringUtils.hasText(text)) return 0;
        // 中文按1.5字/token、英文按4字符/token粗估
        return (int) (text.length() * 0.5);
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }
}
