package com.govai.supervision.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 角色新增/更新请求 DTO
 */
@Data
@Schema(description = "角色编辑请求")
public class RoleDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "角色ID（新增时为空）")
    private Long id;

    @NotBlank(message = "角色名称不能为空")
    @Schema(description = "角色名称")
    private String roleName;

    @NotBlank(message = "角色编码不能为空")
    @Schema(description = "角色编码(唯一)：ADMIN/APPROVER/SUPERVISOR/CITIZEN/ENTERPRISE")
    private String roleCode;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "状态 1启用 0停用")
    private Integer status;
}