package com.govai.supervision.interceptor;

import com.govai.supervision.common.BusinessException;
import com.govai.supervision.util.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 认证拦截器
 * 除 /api/v1/auth/** 外，所有接口均需携带有效 Bearer Token
 * 解析后把 userId / userName / role 放入 request attribute，便于业务侧取值
 */
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    public static final String ATTR_USER_ID = "loginUserId";
    public static final String ATTR_USER_NAME = "loginUserName";
    public static final String ATTR_ROLE = "loginRole";

    private final JwtUtils jwtUtils;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new BusinessException(401, "未登录或 Token 缺失");
        }
        Claims claims = jwtUtils.parseToken(authorization.substring(7));
        if (claims == null) {
            throw new BusinessException(401, "Token 无效或已过期");
        }
        // 写入 attributes 供下游使用
        if (claims.get("userId") != null) {
            request.setAttribute(ATTR_USER_ID, ((Number) claims.get("userId")).longValue());
        }
        if (claims.get("userName") != null) {
            request.setAttribute(ATTR_USER_NAME, claims.get("userName"));
        }
        if (claims.get("role") != null) {
            request.setAttribute(ATTR_ROLE, claims.get("role"));
        }
        return true;
    }
}