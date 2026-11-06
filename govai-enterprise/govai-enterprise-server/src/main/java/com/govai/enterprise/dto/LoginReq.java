package com.govai.enterprise.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 企业用户登录请求
 */
@Data
@Schema(description = "登录请求")
public class LoginReq {

    @Schema(description = "登录账号", example = "ent_admin")
    @NotBlank(message = "账号不能为空")
    private String username;

    @Schema(description = "密码", example = "123456")
    @NotBlank(message = "密码不能为空")
    private String password;
}
