package com.govai.enterprise.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 智能问答请求
 */
@Data
@Schema(description = "智能问答请求")
public class QaAskReq {

    @Schema(description = "用户问题")
    @NotBlank(message = "问题不能为空")
    private String question;

    @Schema(description = "所属会话ID(为空则后端自动新建会话)")
    private Long sessionId;
}
