package com.govai.enterprise.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 企业用户表 enterprise_user
 */
@Data
@TableName("enterprise_user")
public class EnterpriseUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录账号 */
    private String username;

    /** BCrypt 密码 */
    private String password;

    /** 所属企业ID */
    private Long enterpriseId;

    /** 所属企业名称（非表字段，跨端全量列表回填） */
    @TableField(exist = false)
    private String enterpriseName;

    /** 真实姓名 */
    private String realName;

    /** 手机号 */
    private String phone;

    /** ADMIN/OPERATOR */
    private String role;

    /** 1正常 0禁用 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
