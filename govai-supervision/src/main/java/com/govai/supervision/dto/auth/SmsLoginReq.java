package com.govai.supervision.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serializable;

/**
 * 短信快捷登录请求（公民端/群众端 SSO 专用）
 * 演示阶段固定验证码 000000，按 sys_user.phone 定位 CITIZEN 账号并签发统一 token
 */
@Data
@Schema(description = "短信快捷登录请求")
public class SmsLoginReq implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "手机号(须已开通 CITIZEN 公民账号)", example = "13800000020")
    private String phone;

    @NotBlank(message = "验证码不能为空")
    @Schema(description = "验证码(演示固定 000000)", example = "000000")
    private String code;
}
