package com.govai.citizen.service;

import com.govai.citizen.dto.AiChatReq;
import com.govai.citizen.dto.AiChatResp;
import com.govai.citizen.feign.ApprovalClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiService {

    private final ApprovalClient approvalClient;
    private final AiLocalService aiLocalService;

    public AiChatResp chat(AiChatReq req) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", req.getMessage());
        body.put("systemPrompt", req.getSystemPrompt());
        try {
            Map<String, Object> data = approvalClient.aiChat(body).getData();
            AiChatResp resp = new AiChatResp();
            resp.setResponse(String.valueOf(data.get("response")));
            Object tc = data.get("tokenCount");
            resp.setTokenCount(tc == null ? 0 : Long.valueOf(String.valueOf(tc)));
            return resp;
        } catch (Exception e) {
            return aiLocalService.answer(req.getMessage());
        }
    }
}
