package com.govai.supervision.aspect;

import com.govai.supervision.annotation.RequirePermission;
import com.govai.supervision.common.BusinessException;
import com.govai.supervision.entity.SysUser;
import com.govai.supervision.interceptor.JwtInterceptor;
import com.govai.supervision.mapper.SysRolePermissionMapper;
import com.govai.supervision.mapper.SysUserMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

/**
 * 权限校验切面
 * 拦截标注 @RequirePermission 的方法，校验当前登录用户所属角色是否拥有对应权限码。
 * 规则：ADMIN 直接放行；否则需在 sys_role_permission 关联的权限码中含注解要求。
 */
@Aspect
@Component
@RequiredArgsConstructor
public class PermissionAspect {

    private static final String ADMIN_ROLE = "ADMIN";

    private final SysUserMapper sysUserMapper;
    private final SysRolePermissionMapper sysRolePermissionMapper;

    @Before("@annotation(com.govai.supervision.annotation.RequirePermission)")
    public void checkPermission(JoinPoint joinPoint) {
        // 取当前请求的用户信息
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attrs != null ? attrs.getRequest() : null;
        if (request == null) {
            return;
        }
        Object roleAttr = request.getAttribute(JwtInterceptor.ATTR_ROLE);
        String role = roleAttr == null ? null : roleAttr.toString();
        // 超管放行
        if (ADMIN_ROLE.equals(role)) {
            return;
        }

        // 反射拿到方法上的注解权限码
        RequirePermission anno = findAnnotation(joinPoint);
        if (anno == null) {
            return;
        }
        String required = anno.value();

        // 拿当前用户角色 id
        Object uidAttr = request.getAttribute(JwtInterceptor.ATTR_USER_ID);
        if (uidAttr == null) {
            throw new BusinessException(401, "未登录");
        }
        Long userId = Long.valueOf(uidAttr.toString());
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || user.getRoleId() == null) {
            throw new BusinessException(403, "无权限访问该功能");
        }
        List<String> codes = sysRolePermissionMapper.selectPermissionCodesByRoleId(user.getRoleId());
        if (codes == null || !codes.contains(required)) {
            throw new BusinessException(403, "无权限执行该操作");
        }
    }

    private RequirePermission findAnnotation(JoinPoint joinPoint) {
        var method = ((org.aspectj.lang.reflect.MethodSignature) joinPoint.getSignature()).getMethod();
        return method.getAnnotation(RequirePermission.class);
    }
}
