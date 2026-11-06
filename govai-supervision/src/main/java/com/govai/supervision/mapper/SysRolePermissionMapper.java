package com.govai.supervision.mapper;

import com.govai.supervision.entity.SysRolePermission;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色-权限关联 Mapper
 * 复合主键，手写 SQL（不继承 BaseMapper）
 */
@Mapper
public interface SysRolePermissionMapper {

    /** 删除某角色的全部权限授权 */
    @Delete("DELETE FROM sys_role_permission WHERE role_id = #{roleId}")
    int deleteByRoleId(@Param("roleId") Long roleId);

    /** 删除某角色的单个权限（用于取消授权后可能残留清理） */
    @Delete("DELETE FROM sys_role_permission WHERE role_id = #{roleId} AND permission_id = #{permissionId}")
    int deleteOne(@Param("roleId") Long roleId, @Param("permissionId") Long permissionId);

    /** 插入一条授权 */
    @Insert("INSERT INTO sys_role_permission (role_id, permission_id) VALUES (#{roleId}, #{permissionId})")
    int insert(@Param("roleId") Long roleId, @Param("permissionId") Long permissionId);

    /** 查询某角色已授权的权限ID集合 */
    @Select("SELECT permission_id FROM sys_role_permission WHERE role_id = #{roleId}")
    List<Long> selectPermissionIdsByRoleId(@Param("roleId") Long roleId);

    /** 查询某角色已授权的权限编码集合（join sys_permission） */
    @Select("SELECT sp.permission_code FROM sys_role_permission rp "
            + "JOIN sys_permission sp ON rp.permission_id = sp.id "
            + "WHERE rp.role_id = #{roleId} AND sp.permission_code IS NOT NULL")
    List<String> selectPermissionCodesByRoleId(@Param("roleId") Long roleId);
}