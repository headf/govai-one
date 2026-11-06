package com.govai.supervision.service;

import com.govai.supervision.common.PageResult;
import com.govai.supervision.dto.user.UserDTO;
import com.govai.supervision.entity.SysUser;

/**
 * 系统用户服务
 */
public interface SysUserService {

    /** 分页查询用户（可按用户名/姓名/角色模糊，可指定状态；onlyInternal=true 时只查非企业账号(enterprise_id 为空)） */
    PageResult<SysUser> pageUsers(long page, long size, String keyword, Long roleId, Integer status, Boolean onlyInternal);

    /** 查询单用户 */
    SysUser getById(Long id);

    /** 新增用户（密码 BCrypt 加密） */
    SysUser createUser(UserDTO dto);

    /** 更新用户 */
    void updateUser(UserDTO dto);

    /** 删除用户 */
    void deleteUser(Long id);

    /** 重置密码 */
    void resetPassword(Long id, String newPassword);

    /** 更新本人资料（仅姓名/手机/邮箱/部门，不可改角色与状态） */
    void updateProfile(Long userId, UserDTO dto);

    /** 修改本人密码（需校验原密码） */
    void changePassword(Long userId, String oldPassword, String newPassword);
}