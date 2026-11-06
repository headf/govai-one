package com.govai.citizen.dto;

import lombok.Data;

@Data
public class AiChatReq {

    private String message;
    private String systemPrompt;
}
