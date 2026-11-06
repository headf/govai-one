package com.govai.citizen.dto;

import com.govai.citizen.entity.CitizenUser;
import lombok.Data;

@Data
public class UserProfileVO {

    private Long id;
    private String phone;
    private String realName;
    private String idCard;
    private String avatarUrl;
    private Boolean hasIdCard;

    /** 个人信用(来自监管端, 未开通/查询失败为 null) */
    private String creditNo;
    private java.math.BigDecimal creditScore;
    private String creditLevel;

    public static UserProfileVO from(CitizenUser u) {
        UserProfileVO vo = new UserProfileVO();
        vo.setId(u.getId());
        vo.setPhone(u.getPhone());
        vo.setRealName(u.getRealName());
        vo.setIdCard(u.getIdCard());
        vo.setAvatarUrl(u.getAvatarUrl());
        vo.setHasIdCard(u.getIdCard() != null && !u.getIdCard().isBlank());
        return vo;
    }
}
