package com.govai.enterprise.common;

/**
 * 登录用户上下文持有器(ThreadLocal)
 */
public class LoginUserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    /**
     * 获取当前登录用户,不存在则抛 401(无/失效 Token 已由拦截器拦截,此处兜底)
     */
    public static LoginUser require() {
        LoginUser user = HOLDER.get();
        if (user == null) {
            throw new BusinessException(401, "未认证或登录已过期");
        }
        return user;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
