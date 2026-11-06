package com.govai.enterprise.controller;

import com.govai.enterprise.common.LoginUser;
import com.govai.enterprise.common.LoginUserContext;
import com.govai.enterprise.common.Result;
import com.govai.enterprise.dto.LoginResp;
import com.govai.enterprise.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统基础接口:健康检查 / 当前登录用户
 */
@Tag(name = "系统接口")
@RestController
@RequestMapping("/api/v1/sys")
@RequiredArgsConstructor
public class SysController {

    private final AuthService authService;

    @Operation(summary = "健康检查", hidden = true)
    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("govai-enterprise is running");
    }

    @Operation(summary = "当前登录用户")
    @GetMapping("/me")
    public Result<LoginResp> me() {
        LoginUser loginUser = LoginUserContext.require();
        return Result.success(authService.current(loginUser.getUserId()));
    }
}
