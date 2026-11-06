package com.govai.supervision.controller;

import com.govai.supervision.common.Result;
import com.govai.supervision.dto.auth.LoginReq;
import com.govai.supervision.dto.auth.LoginVO;
import com.govai.supervision.dto.auth.SmsLoginReq;
import com.govai.supervision.dto.auth.VerifyVO;
import com.govai.supervision.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器（SSO）
 * 契约 #1 统一登录：POST /api/v1/auth/login
 * 契约 #2 Token 验证：GET /api/v1/auth/verify
 */
@Tag(name = "认证中心（SSO）", description = "统一登录 / Token 验证，供全组调用")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** 统一登录：契约 #1 */
    @Operation(summary = "统一登录", description = "按用户名密码登录，返回 JWT Token")
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody @Valid LoginReq req) {
        return Result.success("登录成功", authService.login(req));
    }

    /** 短信快捷登录(公民端 SSO)：手机号 + 固定验证码 → CITIZEN token */
    @Operation(summary = "短信快捷登录(公民端)", description = "按 sys_user.phone 定位 CITIZEN 账号，固定验证码 000000，签发统一 Token")
    @PostMapping("/sms-login")
    public Result<LoginVO> smsLogin(@RequestBody @Valid SmsLoginReq req) {
        return Result.success("登录成功", authService.smsLogin(req));
    }

    /** Token 验证：契约 #2（全组调用；登录接口本身不拦截） */
    @Operation(summary = "Token 验证", description = "验证 Authorization 头中的 Bearer Token")
    @GetMapping("/verify")
    public Result<VerifyVO> verify(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = resolveToken(authorization);
        return Result.success("验证通过", authService.verify(token));
    }

    /** 从 Authorization 头提取 Bearer token */
    private String resolveToken(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            throw new com.govai.supervision.common.BusinessException(401, "未登录，请先登录");
        }
        if (authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return authorization;
    }
}