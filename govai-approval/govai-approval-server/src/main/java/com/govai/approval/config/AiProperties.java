package com.govai.approval.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 大模型配置
 * 默认走通义千问 DashScope 兼容 OpenAI 协议
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai.model")
public class AiProperties {
    private String apiKey;
    private String baseUrl;
    private String modelName;
    private Double temperature;
    private Integer maxTokens;
    private Integer timeout;
}
