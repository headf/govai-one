package com.govai.citizen.config;

import com.govai.citizen.common.BizException;

/**
 * 当前登录用户上下文(由 JwtInterceptor 填充), 请求结束由拦截器清理
 */
public class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static Long userId() {
        LoginUser u = HOLDER.get();
        return u == null ? null : u.userId();
    }

    public static Long requireUserId() {
        LoginUser u = HOLDER.get();
        if (u == null || u.userId() == null) {
            throw new BizException(401, "未登录或登录已过期");
        }
        return u.userId();
    }

    public static void clear() {
        HOLDER.remove();
    }

    public record LoginUser(Long userId, String userName, String role) {
    }
}
