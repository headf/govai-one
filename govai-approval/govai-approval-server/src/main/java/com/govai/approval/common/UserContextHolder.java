package com.govai.approval.common;

/**
 * 当前登录用户上下文 ThreadLocal 持有者
 */
public final class UserContextHolder {
    private static final ThreadLocal<UserContext> HOLDER = new ThreadLocal<>();

    private UserContextHolder() {}

    public static void set(UserContext ctx) { HOLDER.set(ctx); }
    public static UserContext get() { return HOLDER.get(); }
    public static void clear() { HOLDER.remove(); }

    public static Long currentUserId() {
        UserContext ctx = get();
        return ctx == null ? null : ctx.getUserId();
    }

    public static String currentUserName() {
        UserContext ctx = get();
        return ctx == null ? null : ctx.getUserName();
    }

    public static String currentRole() {
        UserContext ctx = get();
        return ctx == null ? null : ctx.getRole();
    }
}
