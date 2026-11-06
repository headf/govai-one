package com.govai.supervision.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统用户实体（SSO 主表）
 * 全组登录共用此表：sys_user
 */
@Data
@TableName("sys_user")
public class SysUser implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名（唯一） */
    private String username;

    /** 密码（BCrypt 加密） */
    private String password;

    /** 姓名 */
    private String realName;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 部门 */
    private String department;

    /** 角色ID（关联 sys_role） */
    private Long roleId;

    /** 企业账号所属企业（enterprise_info.id；非企业账号为空。SSO 企业端账号专用） */
    private Long enterpriseId;

    /** 角色名（非表字段，列表联查回填） */
    @TableField(exist = false)
    private String roleName;

    /** 状态：1正常 0禁用 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}