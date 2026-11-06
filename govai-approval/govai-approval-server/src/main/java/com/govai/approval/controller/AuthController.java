package com.govai.approval.controller;

import com.govai.approval.common.BusinessException;
import com.govai.approval.common.Constants;
import com.govai.approval.common.Result;
import com.govai.approval.dto.LoginReq;
import com.govai.approval.dto.LoginVO;
import com.govai.approval.feign.SupervisionAuthClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证 Controller
 *
 * 多岗位分离后：登录统一走监管端 SSO（sys_user 主库），按账号角色识别岗位。
 * 审批端可登录岗位：APPROVER(受理/初审) / DECIDE(终审) / ISSUE(办结出证)。
 * 监管端不可用时由 SupervisionAuthClientFallback 以内置岗位账号降级。
 */
@Tag(name = "认证")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SupervisionAuthClient supervisionAuthClient;

    @Operation(summary = "审批员端登录（透传监管端 SSO，校验岗位角色后放行）")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginReq req) {
        // 透传监管端 SSO（不传 loginType，避免被"账号不属于所选登录端"拦截；监管端校验密码/禁用态并签发 JWT）
        Result<LoginVO> remote = supervisionAuthClient.login(req);
        if (remote == null || remote.getCode() != 200 || remote.getData() == null) {
            throw new BusinessException(remote != null && remote.getCode() != 200 ? remote.getCode() : 500,
                    remote != null && StringUtils.hasText(remote.getMessage())
                            ? remote.getMessage() : "监管端登录失败");
        }
        LoginVO data = remote.getData();
        // 审批岗位校验：仅受理/初审(APPROVER)、终审(DECIDE)、办结(ISSUE)可登录审批工作台
        if (!Constants.APPROVAL_STAFF_ROLES.contains(data.getRole())) {
            throw new BusinessException(403, "该账号(" + data.getRole() + ")非审批岗位，请使用受理初审/终审/办结岗位账号登录");
        }
        return Result.success(data);
    }
}
