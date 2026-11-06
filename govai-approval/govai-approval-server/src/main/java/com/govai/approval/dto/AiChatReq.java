package com.govai.approval.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 大模型对话请求体（接口6：POST /api/v1/ai/chat）
 */
@Data
public class AiChatReq {
    @NotBlank(message = "用户消息不能为空")
    private String message;
    /** 系统提示词（可选） */
    private String systemPrompt;
    /** 关联审批实例ID（可选，用于审批辅助） */
    private Long instanceId;
    /** 使用的Prompt模板ID（可选） */
    private Long templateId;
}
