package com.govai.citizen.controller;

import com.govai.citizen.common.Result;
import com.govai.citizen.dto.LoginReq;
import com.govai.citizen.dto.LoginVO;
import com.govai.citizen.dto.WxLoginReq;
import com.govai.citizen.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 手机号一键登录(未注册自动注册), 返回自签JWT
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginReq req) {
        return Result.success(authService.login(req));
    }

    /**
     * 微信小程序登录(openid)
     */
    @PostMapping("/wx-login")
    public Result<LoginVO> wxLogin(@RequestBody WxLoginReq req) {
        return Result.success(authService.wxLogin(req));
    }
}
