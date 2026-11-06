package com.govai.supervision.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 角色授权请求 DTO
 */
@Data
@Schema(description = "角色授权请求")
public class GrantPermissionReq implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "角色ID不能为空")
    @Schema(description = "角色ID")
    private Long roleId;

    @Schema(description = "授权权限ID集合（全量覆盖）")
    private List<Long> permissionIds;
}