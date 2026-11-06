package com.govai.enterprise.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LLM 大模型客户端(OpenAI 兼容接口,commandcode.ai)
 * 用于智能问答:基于检索到的政务知识库 + 会话历史,生成自然语言答案。
 * 调用失败返回 null,由 QaService 兜底本地 FAQ,保证问答主流程可用。
 */
@Slf4j
@Component
public class LlmClient {

    private final RestClient restClient;
    private final String model;

    public LlmClient(@Value("${govai.llm.base-url}") String baseUrl,
                     @Value("${govai.llm.api-key}") String apiKey,
                     @Value("${govai.llm.model}") String model) {
        this.model = model;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /**
     * 对话接口(非流式)
     *
     * @param systemPrompt 系统提示(含 RAG 知识库上下文)
     * @param history      会话历史(最近几轮,user/assistant 交替,老的在前)
     * @param userQuestion 当前问题
     * @return LLM 生成的答案;调用失败返回 null
     */
    @SuppressWarnings("unchecked")
    public String chat(String systemPrompt, List<Map<String, String>> history, String userQuestion) {
        List<Map<String, String>> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(Map.of("role", "system", "content", systemPrompt));
        }
        if (history != null) {
            messages.addAll(history);
        }
        messages.add(Map.of("role", "user", "content", userQuestion));

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("stream", false);

        try {
            Map<String, Object> resp = restClient.post()
                    .uri("/chat/completions")
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            if (resp == null) {
                return null;
            }
            List<Map<String, Object>> choices = (List<Map<String, Object>>) resp.get("choices");
            if (choices == null || choices.isEmpty()) {
                return null;
            }
            Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
            if (msg == null) {
                return null;
            }
            Object content = msg.get("content");
            return content == null ? null : content.toString();
        } catch (Exception e) {
            log.warn("LLM 调用失败,将兜底本地 FAQ: {}", e.getMessage());
            return null;
        }
    }
}
