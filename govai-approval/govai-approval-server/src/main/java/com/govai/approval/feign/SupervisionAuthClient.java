package com.govai.approval.feign;

import com.govai.approval.common.Result;
import com.govai.approval.dto.LoginReq;
import com.govai.approval.dto.LoginVO;
import com.govai.approval.dto.TokenVerifyVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * 监管端认证 Feign 客户端
 *
 * 接口1：POST /api/v1/auth/login   统一登录（前端登录按钮 -> 审批端透传 -> 监管端 SSO）
 * 接口2：GET  /api/v1/auth/verify  Token验证（开发阶段本端用共享密钥本地验证，无需调用）
 *
 * 开发阶段：本端通过共享密钥本地验证 Token，不依赖监管端在线
 * 联调阶段：可改为调用监管端 verify 接口做交叉校验
 */
@FeignClient(
        name = "govai-supervision",
        url = "${feign.targets.supervision:http://localhost:8081}",
        fallback = SupervisionAuthClientFallback.class
)
public interface SupervisionAuthClient {

    @PostMapping("/api/v1/auth/login")
    Result<LoginVO> login(@RequestBody LoginReq req);

    @GetMapping("/api/v1/auth/verify")
    Result<TokenVerifyVO> verify(@RequestHeader("Authorization") String token);
}
