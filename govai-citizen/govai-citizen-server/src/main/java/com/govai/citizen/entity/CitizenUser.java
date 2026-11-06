package com.govai.citizen.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("citizen_user")
public class CitizenUser {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String openid;
    private String phone;
    /** 监管端 sys_user.id(SSO 透传 token 后用于映射回本地用户) */
    private Long sysUserId;
    private String realName;
    private String idCard;
    private String avatarUrl;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
