package com.govai.approval.dto;

import lombok.Data;

/**
 * Token 验证响应（接口2：GET /api/v1/auth/verify）
 * 由监管端提供，全组通过 Feign 调用
 */
@Data
public class TokenVerifyVO {
    private Long userId;
    private String userName;
    private String role;
}
