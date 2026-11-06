package com.govai.enterprise.common;

import lombok.Data;

/**
 * 当前登录用户上下文(由 JWT 拦截器解析后写入 ThreadLocal)
 */
@Data
public class LoginUser {

    /** 用户ID */
    private Long userId;

    /** 登录账号 */
    private String userName;

    /** 真实姓名 */
    private String realName;

    /** 企业端内部角色 ADMIN/OPERATOR */
    private String role;

    /** 所属企业ID(空 = 未绑定企业) */
    private Long enterpriseId;

    /** 统一登录用户ID(监管端 sys_user.id;企业端业务不用,备用/审计) */
    private Long sysUserId;

    public LoginUser() {
    }

    public LoginUser(Long userId, String userName, String realName, String role, Long enterpriseId) {
        this.userId = userId;
        this.userName = userName;
        this.realName = realName;
        this.role = role;
        this.enterpriseId = enterpriseId;
    }
}
