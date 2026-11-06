package com.govai.supervision.controller;

import com.govai.supervision.common.Result;
import com.govai.supervision.annotation.RequirePermission;
import com.govai.supervision.dto.user.RoleDTO;
import com.govai.supervision.entity.SysRole;
import com.govai.supervision.service.SysRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 角色管理控制器
 */
@Tag(name = "角色管理", description = "角色 CRUD")
@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {

    private final SysRoleService sysRoleService;

    /** 角色列表 */
    @Operation(summary = "角色列表")
    @GetMapping("/list")
    public Result<List<SysRole>> list(@RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) Integer status) {
        return Result.success(sysRoleService.listRoles(keyword, status));
    }

    /** 角色详情 */
    @Operation(summary = "角色详情")
    @GetMapping("/{id}")
    public Result<SysRole> detail(@PathVariable Long id) {
        return Result.success(sysRoleService.getById(id));
    }

    /** 新增角色 */
    @Operation(summary = "新增角色")
    @RequirePermission("role:add")
    @PostMapping
    public Result<SysRole> create(@RequestBody @Valid RoleDTO dto) {
        return Result.success("新增成功", sysRoleService.createRole(dto));
    }

    /** 更新角色 */
    @Operation(summary = "更新角色")
    @RequirePermission("role:edit")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid RoleDTO dto) {
        dto.setId(id);
        sysRoleService.updateRole(dto);
        return Result.success();
    }

    /** 删除角色 */
    @Operation(summary = "删除角色")
    @RequirePermission("role:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sysRoleService.deleteRole(id);
        return Result.success();
    }
}