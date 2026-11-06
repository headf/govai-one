package com.govai.supervision.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 统一登录请求
 * 契约 #1：POST /api/v1/auth/login
 */
@Data
@Schema(description = "登录请求")
public class LoginReq implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "用户名不能为空")
    @Schema(description = "用户名", example = "citizen1")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Schema(description = "密码", example = "123456")
    private String password;

    /** 登录类型，可选：ADMIN/SUPERVISOR/APPROVER/CITIZEN/ENTERPRISE */
    @Schema(description = "登录端类型：ADMIN/SUPERVISOR/APPROVER/CITIZEN/ENTERPRISE", example = "CITIZEN")
    private String loginType;
}