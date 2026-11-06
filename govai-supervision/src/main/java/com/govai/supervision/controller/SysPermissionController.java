package com.govai.supervision.controller;

import com.govai.supervision.common.Result;
import com.govai.supervision.annotation.RequirePermission;
import com.govai.supervision.dto.user.GrantPermissionReq;
import com.govai.supervision.entity.SysPermission;
import com.govai.supervision.service.SysPermissionService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 权限管理控制器
 * 契约命名：/api/v1/permissions
 */
@Tag(name = "权限管理", description = "权限树 / 角色授权 / 权限 CRUD")
@RestController
@RequestMapping("/api/v1/permissions")
@RequiredArgsConstructor
public class SysPermissionController {

    private final SysPermissionService sysPermissionService;

    /** 权限树 */
    @Operation(summary = "权限树")
    @GetMapping("/tree")
    public Result<List<SysPermission>> tree() {
        return Result.success(sysPermissionService.tree());
    }

    /** 查询某角色已授权的权限ID */
    @Operation(summary = "角色已授权权限ID")
    @GetMapping("/role/{roleId}")
    public Result<List<Long>> permissionIdsOfRole(@PathVariable Long roleId) {
        return Result.success(sysPermissionService.permissionIdsOfRole(roleId));
    }

    /** 角色授权（全量覆盖） */
    @Operation(summary = "角色授权")
    @RequirePermission("role:grant")
    @PostMapping("/grant")
    public Result<Void> grant(@RequestBody @Valid GrantPermissionReq req) {
        sysPermissionService.grantByRole(req);
        return Result.success();
    }

    /** 新增权限 */
    @Operation(summary = "新增权限")
    @RequirePermission("perm:add")
    @PostMapping
    public Result<SysPermission> create(@RequestBody SysPermission permission) {
        return Result.success("新增成功", sysPermissionService.createPermission(permission));
    }

    /** 更新权限 */
    @Operation(summary = "更新权限")
    @RequirePermission("perm:edit")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody SysPermission permission) {
        permission.setId(id);
        sysPermissionService.updatePermission(permission);
        return Result.success();
    }

    /** 删除权限 */
    @Operation(summary = "删除权限")
    @RequirePermission("perm:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sysPermissionService.deletePermission(id);
        return Result.success();
    }
}