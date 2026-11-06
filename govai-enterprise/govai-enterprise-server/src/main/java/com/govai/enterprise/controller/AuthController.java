package com.govai.enterprise.controller;

import com.govai.enterprise.common.Result;
import com.govai.enterprise.dto.LoginReq;
import com.govai.enterprise.dto.LoginResp;
import com.govai.enterprise.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 企业端认证接口(路径前缀 ent-auth,与群众端/监管端 auth 区分)
 */
@Tag(name = "企业认证")
@RestController
@RequestMapping("/api/v1/ent-auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "企业用户登录", description = "账号+密码登录,返回 JWT(开发期本地签发,与全组共享密钥)")
    @PostMapping("/login")
    public Result<LoginResp> login(@Valid @RequestBody LoginReq req) {
        return Result.success(authService.login(req));
    }
}
