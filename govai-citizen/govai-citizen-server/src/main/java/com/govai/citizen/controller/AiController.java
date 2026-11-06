package com.govai.citizen.controller;

import com.govai.citizen.common.Result;
import com.govai.citizen.dto.AiChatReq;
import com.govai.citizen.dto.AiChatResp;
import com.govai.citizen.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    /**
     * AI 政务助手对话(Feign 调审批端 /api/v1/ai/chat, 不可用时本地规则答复)
     */
    @PostMapping("/chat")
    public Result<AiChatResp> chat(@RequestBody AiChatReq req) {
        return Result.success(aiService.chat(req));
    }
}
