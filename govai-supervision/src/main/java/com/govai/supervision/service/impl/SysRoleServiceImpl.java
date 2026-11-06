package com.govai.supervision.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.govai.supervision.common.BusinessException;
import com.govai.supervision.dto.user.RoleDTO;
import com.govai.supervision.entity.SysRole;
import com.govai.supervision.entity.SysUser;
import com.govai.supervision.mapper.SysRoleMapper;
import com.govai.supervision.mapper.SysUserMapper;
import com.govai.supervision.service.SysRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 角色服务实现
 */
@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

    private final SysUserMapper sysUserMapper;

    @Override
    public List<SysRole> listRoles(String keyword, Integer status) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<SysRole>()
                .like(StringUtils.hasText(keyword), SysRole::getRoleName, keyword)
                .eq(status != null, SysRole::getStatus, status)
                .orderByAsc(SysRole::getId);
        return list(wrapper);
    }

    @Override
    public SysRole createRole(RoleDTO dto) {
        Long cnt = baseMapper.selectCount(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, dto.getRoleCode()));
        if (cnt != null && cnt > 0) {
            throw new BusinessException(400, "角色编码已存在");
        }
        SysRole role = new SysRole();
        copyFromDto(role, dto);
        role.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        save(role);
        return role;
    }

    @Override
    public void updateRole(RoleDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(400, "角色ID不能为空");
        }
        SysRole exist = getById(dto.getId());
        if (exist == null) {
            throw new BusinessException(404, "角色不存在");
        }
        if (StringUtils.hasText(dto.getRoleCode()) && !dto.getRoleCode().equals(exist.getRoleCode())) {
            Long cnt = baseMapper.selectCount(
                    new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, dto.getRoleCode()));
            if (cnt != null && cnt > 0) {
                throw new BusinessException(400, "角色编码已存在");
            }
        }
        SysRole role = new SysRole();
        role.setId(dto.getId());
        copyFromDto(role, dto);
        updateById(role);
    }

    @Override
    public void deleteRole(Long id) {
        SysRole exist = getById(id);
        if (exist == null) {
            throw new BusinessException(404, "角色不存在");
        }
        // 有用户引用的角色禁止删除
        Long refUsers = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getRoleId, id));
        if (refUsers != null && refUsers > 0) {
            throw new BusinessException(400, "该角色下仍有用户，无法删除");
        }
        removeById(id);
    }

    private void copyFromDto(SysRole role, RoleDTO dto) {
        role.setRoleName(dto.getRoleName());
        role.setRoleCode(dto.getRoleCode());
        role.setDescription(dto.getDescription());
        if (dto.getStatus() != null) {
            role.setStatus(dto.getStatus());
        }
    }
}