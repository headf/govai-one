package com.govai.citizen.interceptor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.govai.citizen.common.Constants;
import com.govai.citizen.common.JwtUtils;
import com.govai.citizen.config.GovaiProperties;
import com.govai.citizen.config.UserContext;
import com.govai.citizen.entity.CitizenUser;
import com.govai.citizen.mapper.CitizenUserMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 拦截器: Bearer 解析 + 本地共享密钥验签。
 * 放行: 登录(携带公开 GET /api/v1/matters 列表/分类/详情 —— 审批端契约拉取无需登录)
 * 其余接口一律要求有效 token。
 */
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final GovaiProperties props;
    private final CitizenUserMapper citizenUserMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        String uri = request.getRequestURI();
        // 登录接口
        if (uri.startsWith("/api/v1/auth/")) {
            return true;
        }
        // 事项公开读接口(列表/分类/详情): 仅 GET 放行, 写操作仍需认证
        if (HttpMethod.GET.matches(request.getMethod()) && isPublicMatterRead(uri)) {
            return true;
        }
        String header = request.getHeader(props.getJwt().getHeader());
        if (header == null || !header.startsWith(props.getJwt().getPrefix())) {
            return reject(response);
        }
        String token = header.substring(props.getJwt().getPrefix().length());
        Claims claims = JwtUtils.parse(props.getJwt().getSecret(), token);
        if (claims == null) {
            return reject(response);
        }
        Object uid = claims.get(Constants.CLAIM_USER_ID);
        if (uid == null) {
            return reject(response);
        }
        Object name = claims.get(Constants.CLAIM_USER_NAME);
        Object role = claims.get(Constants.CLAIM_ROLE);
        Long tokenUserId = Long.valueOf(String.valueOf(uid));

        Long localUserId = tokenUserId;
        String localName = name == null ? null : String.valueOf(name);
        // SSO 模式: token 由监管端签发, userId=sys_user.id, 需映射回本地 citizen_user.id
        if (!props.isMockLogin()) {
            CitizenUser local = citizenUserMapper.selectOne(new LambdaQueryWrapper<CitizenUser>()
                    .eq(CitizenUser::getSysUserId, tokenUserId)
                    .last("limit 1"));
            if (local == null) {
                return reject(response);
            }
            localUserId = local.getId();
            localName = local.getRealName() == null ? local.getPhone() : local.getRealName();
        }
        UserContext.set(new UserContext.LoginUser(
                localUserId,
                localName,
                role == null ? null : String.valueOf(role)));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private boolean isPublicMatterRead(String uri) {
        return uri.startsWith("/api/v1/matters/list")
                || uri.startsWith("/api/v1/matters/categories")
                || uri.matches("/api/v1/matters/\\d+");
    }

    private boolean reject(HttpServletResponse response) throws Exception {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"未认证或登录已过期\",\"data\":null}");
        return false;
    }
}
