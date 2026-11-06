package com.govai.supervision.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户新增/更新请求 DTO
 */
@Data
@Schema(description = "用户编辑请求")
public class UserDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID（新增时为空）")
    private Long id;

    @NotBlank(message = "用户名不能为空")
    @Schema(description = "用户名(唯一)")
    private String username;

    /** 新增时必填，更新时若为空则不修改密码 */
    @Schema(description = "密码(新增必填), BCrypt存储")
    private String password;

    @NotBlank(message = "姓名不能为空")
    @Schema(description = "姓名")
    private String realName;

    @Pattern(regexp = "^$|^1\\d{10}$", message = "手机号格式不正确")
    @Schema(description = "手机号")
    private String phone;

    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "部门")
    private String department;

    @Schema(description = "角色ID(关联 sys_role)")
    private Long roleId;

    @Schema(description = "企业账号所属企业ID(enterprise_info.id;仅当该账号为企业账号时填写)")
    private Long enterpriseId;

    @Schema(description = "状态 1正常 0禁用")
    private Integer status;
}