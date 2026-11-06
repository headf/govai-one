package com.govai.approval.interceptor;

import com.govai.approval.common.Constants;
import com.govai.approval.common.JwtUtils;
import com.govai.approval.common.UserContext;
import com.govai.approval.common.UserContextHolder;
import com.govai.approval.config.JwtProperties;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * JWT 拦截器
 * 本地验证 Token（密钥与监管端一致），开发阶段不需要调监管端 verify 接口
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtProperties jwtProperties;

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) throws IOException {
        // 从请求头取 Token
        String header = req.getHeader(jwtProperties.getHeader());
        if (!StringUtils.hasText(header) || !header.startsWith(jwtProperties.getPrefix())) {
            sendUnauthorized(resp, 401, "未登录或Token缺失");
            return false;
        }
        String token = header.substring(jwtProperties.getPrefix().length()).trim();
        Claims claims = JwtUtils.parseSafely(token, jwtProperties.getSecret());
        if (claims == null) {
            sendUnauthorized(resp, 401, "Token无效或已过期");
            return false;
        }
        // 注入当前用户
        Long userId = claims.get(Constants.CLAIM_USER_ID, Long.class);
        if (userId == null) {
            // JSON 反序列化 Long 可能存为 Integer
            Object raw = claims.get(Constants.CLAIM_USER_ID);
            if (raw instanceof Number n) userId = n.longValue();
        }
        String userName = claims.get(Constants.CLAIM_USER_NAME, String.class);
        String role = claims.get(Constants.CLAIM_ROLE, String.class);
        UserContextHolder.set(new UserContext(userId, userName, role));
        req.setAttribute(Constants.CLAIM_USER_ID, userId);
        req.setAttribute(Constants.CLAIM_USER_NAME, userName);
        req.setAttribute(Constants.CLAIM_ROLE, role);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest req, HttpServletResponse resp, Object handler, Exception ex) {
        UserContextHolder.clear();
    }

    private void sendUnauthorized(HttpServletResponse resp, int code, String message) throws IOException {
        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        resp.setContentType("application/json;charset=UTF-8");
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.getWriter().write("{\"code\":" + code + ",\"message\":\"" + message + "\",\"data\":null}");
    }
}
