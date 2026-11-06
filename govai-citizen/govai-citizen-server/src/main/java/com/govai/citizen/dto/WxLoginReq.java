package com.govai.citizen.dto;

import lombok.Data;

@Data
public class WxLoginReq {

    private String openid;
    private String nickname;
    private String avatarUrl;
}
