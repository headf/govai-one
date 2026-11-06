package com.govai.approval.common;

import lombok.Data;

/**
 * 当前登录用户上下文（由 JwtInterceptor 从 Token 解析后注入）
 */
@Data
public class UserContext {
    private Long userId;
    private String userName;
    private String role;

    public UserContext(Long userId, String userName, String role) {
        this.userId = userId;
        this.userName = userName;
        this.role = role;
    }
}
