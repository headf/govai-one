package com.govai.supervision.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.govai.supervision.common.BusinessException;
import com.govai.supervision.dto.user.GrantPermissionReq;
import com.govai.supervision.entity.SysPermission;
import com.govai.supervision.entity.SysRole;
import com.govai.supervision.mapper.SysPermissionMapper;
import com.govai.supervision.mapper.SysRoleMapper;
import com.govai.supervision.mapper.SysRolePermissionMapper;
import com.govai.supervision.service.SysPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 权限服务实现
 */
@Service
@RequiredArgsConstructor
public class SysPermissionServiceImpl implements SysPermissionService {

    private final SysPermissionMapper sysPermissionMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysRolePermissionMapper sysRolePermissionMapper;

    @Override
    public List<SysPermission> tree() {
        List<SysPermission> all = sysPermissionMapper.selectList(
                new LambdaQueryWrapper<SysPermission>().orderByAsc(SysPermission::getSortOrder));
        // 组装树
        Map<Long, List<SysPermission>> byParent = all.stream()
                .collect(Collectors.groupingBy(p ->
                        p.getParentId() == null ? 0L : p.getParentId()));
        for (SysPermission p : all) {
            p.setChildren(byParent.getOrDefault(p.getId(), Collections.emptyList()));
        }
        List<SysPermission> roots = byParent.getOrDefault(0L, Collections.emptyList());
        return roots;
    }

    @Override
    public List<Long> permissionIdsOfRole(Long roleId) {
        return sysRolePermissionMapper.selectPermissionIdsByRoleId(roleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grantByRole(GrantPermissionReq req) {
        SysRole role = sysRoleMapper.selectById(req.getRoleId());
        if (role == null) {
            throw new BusinessException(404, "角色不存在");
        }
        // 先清空该角色旧授权，再全量写入
        sysRolePermissionMapper.deleteByRoleId(req.getRoleId());
        if (req.getPermissionIds() != null) {
            for (Long pid : req.getPermissionIds()) {
                if (pid != null) {
                    sysRolePermissionMapper.insert(req.getRoleId(), pid);
                }
            }
        }
    }

    @Override
    public SysPermission createPermission(SysPermission permission) {
        Long cnt = sysPermissionMapper.selectCount(new LambdaQueryWrapper<SysPermission>()
                .eq(SysPermission::getPermissionCode, permission.getPermissionCode()));
        if (cnt != null && cnt > 0) {
            throw new BusinessException(400, "权限编码已存在");
        }
        permission.setId(null);
        if (permission.getParentId() == null) {
            permission.setParentId(0L);
        }
        if (permission.getSortOrder() == null) {
            permission.setSortOrder(0);
        }
        sysPermissionMapper.insert(permission);
        return permission;
    }

    @Override
    public void updatePermission(SysPermission permission) {
        if (permission.getId() == null) {
            throw new BusinessException(400, "权限ID不能为空");
        }
        if (sysPermissionMapper.selectById(permission.getId()) == null) {
            throw new BusinessException(404, "权限不存在");
        }
        sysPermissionMapper.updateById(permission);
    }

    @Override
    public void deletePermission(Long id) {
        if (sysPermissionMapper.selectById(id) == null) {
            throw new BusinessException(404, "权限不存在");
        }
        Long children = sysPermissionMapper.selectCount(new LambdaQueryWrapper<SysPermission>()
                .eq(SysPermission::getParentId, id));
        if (children != null && children > 0) {
            throw new BusinessException(400, "存在子权限，无法直接删除");
        }
        sysPermissionMapper.deleteById(id);
    }
}