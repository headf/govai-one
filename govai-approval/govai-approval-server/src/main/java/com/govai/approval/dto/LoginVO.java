package com.govai.approval.dto;

import lombok.Data;

/**
 * 登录响应
 */
@Data
public class LoginVO {
    private String token;
    private Long userId;
    private String userName;
    private String role;
    private Long expireIn;
}
