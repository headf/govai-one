package com.govai.enterprise.interceptor;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.govai.enterprise.common.BusinessException;
import com.govai.enterprise.common.JwtUtils;
import com.govai.enterprise.common.LoginUser;
import com.govai.enterprise.common.LoginUserContext;
import com.govai.enterprise.entity.EnterpriseUser;
import com.govai.enterprise.mapper.EnterpriseUserMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 登录拦截器
 * 统一 SSO 改造后：token 由监管端 /auth/login 签发，载荷 userId=sys_user.id、含 enterpriseId。
 * 本拦截器不再把 claim 当 enterprise_user.id / ADMIN·OPERATOR，改为按 username 回查本地
 * enterprise_user（角色档案），把 LoginUser.userId 设回 enterprise_user.id 以兼容全端既有业务。
 */
@Component
@RequiredArgsConstructor
public class JwtAuthInterceptor implements HandlerInterceptor {

    private final JwtUtils jwtUtils;
    private final EnterpriseUserMapper enterpriseUserMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // CORS 预检直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String token = request.getHeader("Authorization");
        if (StrUtil.isBlank(token) || !token.startsWith("Bearer ")) {
            throw new BusinessException(401, "未认证,请先登录");
        }
        Claims claims = jwtUtils.parse(token.substring(7));
        if (claims == null) {
            throw new BusinessException(401, "Token 无效或已过期");
        }

        // 企业端只接受带企业归属(enterpriseId claim)的统一登录 token
        Long enterpriseId = claims.get("enterpriseId") == null
                ? null : ((Number) claims.get("enterpriseId")).longValue();
        if (enterpriseId == null) {
            throw new BusinessException(401, "非企业账号,无法访问企业端");
        }
        String username = (String) claims.get("userName");

        // 回查本地企业角色档案(enterprise_user.username 全库唯一),校验归属防越企
        EnterpriseUser entUser = enterpriseUserMapper.selectOne(
                new LambdaQueryWrapper<EnterpriseUser>()
                        .eq(EnterpriseUser::getUsername, username));
        if (entUser == null || !enterpriseId.equals(entUser.getEnterpriseId())) {
            throw new BusinessException(401, "企业账号档案缺失或与企业归属不符,请联系企业法人");
        }

        LoginUser loginUser = new LoginUser(
                entUser.getId(),          // 回设 enterprise_user.id,兼容全端既有 operator_id/创建人语义
                username,
                entUser.getRealName(),
                entUser.getRole(),        // ADMIN/OPERATOR 来自本地档案
                enterpriseId              // 以 token claim 为准
        );
        loginUser.setSysUserId(((Number) claims.get("userId")).longValue()); // sys_user.id 备用
        LoginUserContext.set(loginUser);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        LoginUserContext.clear();
    }
}
