package com.govai.supervision.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 登录响应
 * 契约 #1：POST /api/v1/auth/login 返回体 data
 */
@Data
@NoArgsConstructor
@Schema(description = "登录响应")
public class LoginVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "JWT Token")
    private String token;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名/显示名")
    private String userName;

    @Schema(description = "角色编码：CITIZEN/ENTERPRISE/APPROVER/SUPERVISOR/ADMIN")
    private String role;

    @Schema(description = "登录账号(与 userName 的差异：userName 可能是真实姓名)")
    private String account;

    @Schema(description = "企业账号所属企业ID(enterprise_info.id,非企业账号为空)")
    private Long enterpriseId;

    @Schema(description = "过期时间（秒）", example = "604800")
    private Long expireIn;

    @Schema(description = "该角色已授权的权限码列表（供前端控制菜单/按钮）")
    private List<String> permissions;

    public LoginVO(String token, Long userId, String userName, String role, Long expireIn) {
        this.token = token;
        this.userId = userId;
        this.userName = userName;
        this.role = role;
        this.expireIn = expireIn;
    }
}
