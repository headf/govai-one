package com.govai.approval.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求（前端登录 -> 调监管端 SSO）
 */
@Data
public class LoginReq {
    @NotBlank(message = "用户名不能为空")
    private String username;
    @NotBlank(message = "密码不能为空")
    private String password;
    /** 登录类型 APPROVER/SUPERVISOR/ADMIN */
    private String loginType;
}
