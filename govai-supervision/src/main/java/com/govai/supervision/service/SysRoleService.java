package com.govai.supervision.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.govai.supervision.dto.user.RoleDTO;
import com.govai.supervision.entity.SysRole;

import java.util.List;

/**
 * 角色服务
 */
public interface SysRoleService extends IService<SysRole> {

    /** 角色列表（不含敏感字段校验用） */
    List<SysRole> listRoles(String keyword, Integer status);

    /** 新增角色 */
    SysRole createRole(RoleDTO dto);

    /** 更新角色 */
    void updateRole(RoleDTO dto);

    /** 删除角色（有用户引用则禁止） */
    void deleteRole(Long id);
}