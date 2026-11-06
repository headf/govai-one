package com.govai.supervision.service;

import com.govai.supervision.dto.user.GrantPermissionReq;
import com.govai.supervision.entity.SysPermission;

import java.util.List;

/**
 * 权限服务
 */
public interface SysPermissionService {

    /** 权限树（含一键展开） */
    List<SysPermission> tree();

    /** 角色已授权的权限ID列表 */
    List<Long> permissionIdsOfRole(Long roleId);

    /** 给角色授权（全量覆盖：先清后插） */
    void grantByRole(GrantPermissionReq req);

    /** 新增权限 */
    SysPermission createPermission(SysPermission permission);

    /** 更新权限 */
    void updatePermission(SysPermission permission);

    /** 删除权限 */
    void deletePermission(Long id);
}