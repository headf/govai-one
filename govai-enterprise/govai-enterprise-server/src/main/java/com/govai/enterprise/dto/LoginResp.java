package com.govai.enterprise.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 登录响应
 */
@Data
@Schema(description = "登录响应")
public class LoginResp {

    @Schema(description = "JWT Token")
    private String token;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "真实姓名")
    private String userName;

    @Schema(description = "登录账号")
    private String account;

    @Schema(description = "企业端角色 ADMIN/OPERATOR")
    private String role;

    @Schema(description = "所属企业ID")
    private Long enterpriseId;

    @Schema(description = "企业名称")
    private String enterpriseName;

    @Schema(description = "有效期(秒)")
    private Long expireIn;
}
