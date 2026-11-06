package com.govai.approval.config;

import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * LangChain4j 大模型客户端配置
 *
 * 通义千问 DashScope 兼容 OpenAI 协议：
 *   baseUrl = https://dashscope.aliyuncs.com/compatible-mode/v1
 *   modelName = qwen-plus
 *
 * 当 api-key 为占位符时降级为 Mock 客户端，保证未配置密钥时项目仍可启动
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AiClientConfig {

    private final AiProperties aiProperties;

    /**
     * OpenAI 兼容的对话模型 Bean
     * 业务层通过该 Bean 调用大模型，避免 LangChain4j 版本细节侵入
     */
    @Bean
    public OpenAiChatModel openAiChatModel() {
        if (isPlaceholderKey(aiProperties.getApiKey())) {
            log.warn("AI api-key 未配置（占位符），AI 对话将返回 Mock 响应。请在 application.yml 中配置 ai.model.api-key");
            return null;  // AiAssistService 会处理 null 情况
        }
        return OpenAiChatModel.builder()
                .apiKey(aiProperties.getApiKey())
                .baseUrl(aiProperties.getBaseUrl())
                .modelName(aiProperties.getModelName())
                .temperature(aiProperties.getTemperature() == null ? 0.3 : aiProperties.getTemperature())
                .maxTokens(aiProperties.getMaxTokens() == null ? 2000 : aiProperties.getMaxTokens())
                .timeout(Duration.ofSeconds(aiProperties.getTimeout() == null ? 60 : aiProperties.getTimeout()))
                .build();
    }

    private boolean isPlaceholderKey(String key) {
        return key == null || key.isBlank() || key.contains("placeholder") || key.contains("your-");
    }
}
