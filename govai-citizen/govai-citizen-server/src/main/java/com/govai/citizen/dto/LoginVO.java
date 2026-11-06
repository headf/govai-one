package com.govai.citizen.dto;

import lombok.Data;

@Data
public class LoginVO {

    private String token;
    private Long userId;
    private String userName;
    private String role;
    private Long expireIn;
    private String phone;
    private String avatarUrl;
}
