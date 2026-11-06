package com.govai.approval.feign;

import com.govai.approval.common.Constants;
import com.govai.approval.common.JwtUtils;
import com.govai.approval.common.Result;
import com.govai.approval.config.JwtProperties;
import com.govai.approval.dto.LoginReq;
import com.govai.approval.dto.LoginVO;
import com.govai.approval.dto.TokenVerifyVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 监管端认证 Feign 降级
 *
 * 监管端不可用时，登录接口本地签发 JWT（共享密钥），保证审批端可独立启动
 * 开发阶段：本端通过共享密钥本地验证 Token
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SupervisionAuthClientFallback implements SupervisionAuthClient {

    private final JwtProperties jwtProperties;

    @Override
    public Result<LoginVO> login(LoginReq req) {
        log.warn("监管端 SSO 不可用，审批端按内置岗位账号本地签发降级。username={}", req.getUsername());
        // 多岗位分离：监管端不可用时仅内置岗位账号可登录（受理/初审 approver1、终审 decide1、办结 issue1）
        String role = demoRole(req.getUsername());
        if (role == null) {
            return Result.fail(403, "监管端不可用，仅内置岗位账号(approver1/decide1/issue1,密码123456)可登录");
        }
        // 密码仅支持内置 123456（离线降级不对真实密码做校验）
        if (!"123456".equals(req.getPassword())) {
            return Result.fail(400, "用户名或密码错误");
        }
        Long userId = demoUserId(req.getUsername());
        String token = JwtUtils.sign(userId, req.getUsername(), role,
                jwtProperties.getSecret(), jwtProperties.getExpire());
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setUserId(userId);
        vo.setUserName(req.getUsername());
        vo.setRole(role);
        vo.setExpireIn(jwtProperties.getExpire());
        return Result.success(vo);
    }

    /** 内置岗位账号 -> 角色映射；非内置账号返回 null */
    private String demoRole(String username) {
        return switch (username == null ? "" : username) {
            case "approver1" -> Constants.ROLE_APPROVER;
            case "decide1" -> Constants.ROLE_DECIDE;
            case "issue1" -> Constants.ROLE_ISSUE;
            default -> null;
        };
    }

    private Long demoUserId(String username) {
        return switch (username == null ? "" : username) {
            case "approver1" -> 2L;
            case "decide1" -> 9L;
            case "issue1" -> 10L;
            default -> 1L;
        };
    }

    @Override
    public Result<TokenVerifyVO> verify(String token) {
        log.warn("监管端 verify 不可用，但本端已通过共享密钥本地验证，降级返回 null");
        return Result.success(null);
    }
}
