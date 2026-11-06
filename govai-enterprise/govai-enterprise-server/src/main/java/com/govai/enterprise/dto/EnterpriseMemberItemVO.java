package com.govai.enterprise.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 企业成员项(跨服务只读;脱敏,不含密码)
 */
@Data
public class EnterpriseMemberItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String username;

    private String realName;

    private String phone;

    /** ADMIN=法人账号, OPERATOR=经办人 */
    private String role;

    private Integer status;
}
