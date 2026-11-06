package com.govai.supervision.controller;

import com.govai.supervision.common.PageResult;
import com.govai.supervision.common.Result;
import com.govai.supervision.annotation.RequirePermission;
import com.govai.supervision.dto.user.UserDTO;
import com.govai.supervision.entity.SysUser;
import com.govai.supervision.service.SysUserService;
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
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户管理控制器
 * 契约命名：/api/v1/users
 */
@Tag(name = "用户管理", description = "系统用户 CRUD / 重置密码")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final SysUserService sysUserService;

    /** 查询用户列表（分页） */
    @Operation(summary = "用户分页列表")
    @GetMapping("/list")
    public Result<PageResult<SysUser>> list(@RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "10") long size,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) Long roleId,
                                            @RequestParam(required = false) Integer status,
                                            @RequestParam(required = false) Boolean onlyInternal) {
        return Result.success(sysUserService.pageUsers(page, size, keyword, roleId, status, onlyInternal));
    }

    /** 查询单个用户 */
    @Operation(summary = "用户详情")
    @GetMapping("/{id}")
    public Result<SysUser> detail(@PathVariable Long id) {
        return Result.success(sysUserService.getById(id));
    }

    /** 新增用户 */
    @Operation(summary = "新增用户")
    @RequirePermission("user:add")
    @PostMapping
    public Result<SysUser> create(@RequestBody @Valid UserDTO dto) {
        return Result.success("新增成功", sysUserService.createUser(dto));
    }

    /** 更新用户 */
    @Operation(summary = "更新用户")
    @RequirePermission("user:edit")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid UserDTO dto) {
        dto.setId(id);
        sysUserService.updateUser(dto);
        return Result.success();
    }

    /** 删除用户 */
    @Operation(summary = "删除用户")
    @RequirePermission("user:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sysUserService.deleteUser(id);
        return Result.success();
    }

    /** 重置密码（特殊操作命名） */
    @Operation(summary = "重置密码")
    @RequirePermission("user:reset-pwd")
    @PostMapping("/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestParam String newPassword) {
        sysUserService.resetPassword(id, newPassword);
        return Result.success();
    }

    // ===== 个人中心（当前登录人）=====

    /** 查询本人资料 */
    @Operation(summary = "查询本人资料")
    @GetMapping("/profile")
    public Result<SysUser> profile(@RequestAttribute("loginUserId") Long userId) {
        return Result.success(sysUserService.getById(userId));
    }

    /** 更新本人资料（姓名/手机/邮箱/部门） */
    @Operation(summary = "更新本人资料")
    @PutMapping("/profile")
    public Result<Void> updateProfile(@RequestAttribute("loginUserId") Long userId,
                                      @RequestBody @Valid UserDTO dto) {
        sysUserService.updateProfile(userId, dto);
        return Result.<Void>success("资料已更新", null);
    }

    /** 修改本人密码（校验原密码） */
    @Operation(summary = "修改本人密码")
    @PutMapping("/profile/password")
    public Result<Void> changePassword(@RequestAttribute("loginUserId") Long userId,
                                       @RequestParam String oldPassword,
                                       @RequestParam String newPassword) {
        sysUserService.changePassword(userId, oldPassword, newPassword);
        return Result.<Void>success("密码已修改，请重新登录", null);
    }
}