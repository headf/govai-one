package com.govai.citizen.dto;

import lombok.Data;

@Data
public class UpdateProfileReq {

    private String realName;
    private String idCard;
    private String avatarUrl;
}
