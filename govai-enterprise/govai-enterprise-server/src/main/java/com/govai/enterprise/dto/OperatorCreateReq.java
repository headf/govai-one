package com.govai.enterprise.dto;

import lombok.Data;

/**
 * 新增经办人请求(法人添加企业经办人)
 */
@Data
public class OperatorCreateReq {

    /** 登录账号 */
    private String username;

    /** 登录密码 */
    private String password;

    /** 真实姓名 */
    private String realName;

    /** 手机号 */
    private String phone;
}
