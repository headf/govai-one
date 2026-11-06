package com.govai.supervision.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Token 验证响应
 * 契约 #2：GET /api/v1/auth/verify 返回体 data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Token 验证响应")
public class VerifyVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String userName;

    @Schema(description = "角色编码")
    private String role;
}