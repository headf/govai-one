package com.govai.supervision.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.govai.supervision.common.BusinessException;
import com.govai.supervision.common.PageResult;
import com.govai.supervision.dto.user.UserDTO;
import com.govai.supervision.entity.SysUser;
import com.govai.supervision.mapper.SysUserMapper;
import com.govai.supervision.entity.SysRole;
import com.govai.supervision.mapper.SysRoleMapper;
import com.govai.supervision.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 系统用户服务实现
 */
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl implements SysUserService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public PageResult<SysUser> pageUsers(long page, long size, String keyword, Long roleId, Integer status, Boolean onlyInternal) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .isNull(Boolean.TRUE.equals(onlyInternal), SysUser::getEnterpriseId)
                .eq(roleId != null, SysUser::getRoleId, roleId)
                .eq(status != null, SysUser::getStatus, status)
                .and(StringUtils.hasText(keyword), kw -> kw
                        .like(SysUser::getUsername, keyword)
                        .or().like(SysUser::getRealName, keyword));
        wrapper.orderByDesc(SysUser::getCreateTime);

        Page<SysUser> result = sysUserMapper.selectPage(new Page<>(page, size), wrapper);
        // 联查角色名，回填 roleName（避免前端角色列恒空）
        List<Long> roleIds = result.getRecords().stream()
                .map(SysUser::getRoleId).filter(id -> id != null).distinct().collect(Collectors.toList());
        Map<Long, String> roleNameMap = new HashMap<>();
        if (!roleIds.isEmpty()) {
            sysRoleMapper.selectBatchIds(roleIds)
                    .forEach(r -> roleNameMap.put(r.getId(), r.getRoleName()));
        }
        result.getRecords().forEach(u -> {
            u.setRoleName(roleNameMap.get(u.getRoleId()));
            // 密码脱敏，返回给前端不泄露
            u.setPassword(null);
        });
        return PageResult.of(result);
    }

    @Override
    public SysUser getById(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        user.setPassword(null);
        return user;
    }

    @Override
    public SysUser createUser(UserDTO dto) {
        // 用户名唯一校验
        Long exists = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, dto.getUsername()));
        if (exists != null && exists > 0) {
            throw new BusinessException(400, "用户名已存在");
        }
        SysUser user = new SysUser();
        copyUser(user, dto);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));;
        user.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        sysUserMapper.insert(user);
        user.setPassword(null);
        return user;
    }

    @Override
    public void updateUser(UserDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(400, "用户ID不能为空");
        }
        SysUser exist = sysUserMapper.selectById(dto.getId());
        if (exist == null) {
            throw new BusinessException(404, "用户不存在");
        }
        // 改用户名时唯一校验
        if (StringUtils.hasText(dto.getUsername()) && !dto.getUsername().equals(exist.getUsername())) {
            Long cnt = sysUserMapper.selectCount(
                    new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, dto.getUsername()));
            if (cnt != null && cnt > 0) {
                throw new BusinessException(400, "用户名已存在");
            }
        }
        SysUser user = new SysUser();
        user.setId(dto.getId());
        copyUser(user, dto);
        sysUserMapper.updateById(user);
    }

    @Override
    public void deleteUser(Long id) {
        SysUser exist = sysUserMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(404, "用户不存在");
        }
        // 防止误删初始管理员
        if ("admin".equals(exist.getUsername())) {
            throw new BusinessException(403, "初始管理员账号不允许删除");
        }
        sysUserMapper.deleteById(id);
    }

    @Override
    public void resetPassword(Long id, String newPassword) {
        if (id == null || !StringUtils.hasText(newPassword)) {
            throw new BusinessException(400, "ID 或新密码不能为空");
        }
        SysUser exist = sysUserMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(404, "用户不存在");
        }
        SysUser update = new SysUser();
        update.setId(id);
        update.setPassword(passwordEncoder.encode(newPassword));
        sysUserMapper.updateById(update);
    }

    @Override
    public void updateProfile(Long userId, UserDTO dto) {
        if (userId == null) {
            throw new BusinessException(400, "用户ID不能为空");
        }
        SysUser exist = sysUserMapper.selectById(userId);
        if (exist == null) {
            throw new BusinessException(404, "用户不存在");
        }
        SysUser update = new SysUser();
        update.setId(userId);
        update.setRealName(dto.getRealName());
        update.setPhone(dto.getPhone());
        update.setEmail(dto.getEmail());
        update.setDepartment(dto.getDepartment());
        // 不可自行变更角色与状态
        sysUserMapper.updateById(update);
    }

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        if (userId == null) {
            throw new BusinessException(400, "用户ID不能为空");
        }
        if (!StringUtils.hasText(newPassword)) {
            throw new BusinessException(400, "新密码不能为空");
        }
        SysUser exist = sysUserMapper.selectById(userId);
        if (exist == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (!passwordEncoder.matches(oldPassword, exist.getPassword())) {
            throw new BusinessException(400, "原密码不正确");
        }
        SysUser update = new SysUser();
        update.setId(userId);
        update.setPassword(passwordEncoder.encode(newPassword));
        sysUserMapper.updateById(update);
    }

    /** 复制非密码字段 */
    private void copyUser(SysUser user, UserDTO dto) {
        user.setUsername(dto.getUsername());
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setDepartment(dto.getDepartment());
        user.setRoleId(dto.getRoleId());
        user.setEnterpriseId(dto.getEnterpriseId());
        if (dto.getStatus() != null) {
            user.setStatus(dto.getStatus());
        }
    }
}