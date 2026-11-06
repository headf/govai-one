package com.govai.approval.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 大模型对话响应体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiChatVO {
    private String response;
    private Integer tokenCount;
    private Long responseTimeMs;
}
