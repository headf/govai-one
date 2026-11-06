package com.govai.enterprise.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.govai.enterprise.common.BusinessException;
import com.govai.enterprise.common.LoginUser;
import com.govai.enterprise.dto.EnterpriseMemberItemVO;
import com.govai.enterprise.dto.EnterpriseMemberVO;
import com.govai.enterprise.dto.OperatorCreateReq;
import com.govai.enterprise.entity.EnterpriseInfo;
import com.govai.enterprise.entity.EnterpriseUser;
import com.govai.enterprise.mapper.EnterpriseInfoMapper;
import com.govai.enterprise.mapper.EnterpriseUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 企业用户(经办人)管理服务
 * 法人(ADMIN)可管理本企业经办人(OPERATOR);经办人不可操作用户管理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnterpriseUserService {

    private final EnterpriseUserMapper userMapper;
    private final EnterpriseInfoMapper enterpriseInfoMapper;
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    /**
     * 查询本企业全部用户(法人+经办人)
     */
    public List<EnterpriseUser> listByEnterprise(LoginUser user) {
        requireAdmin(user);
        return userMapper.selectList(
                new LambdaQueryWrapper<EnterpriseUser>()
                        .eq(EnterpriseUser::getEnterpriseId, user.getEnterpriseId())
                        .orderByAsc(EnterpriseUser::getRole));
    }

    /**
     * 法人新增经办人
     */
    @Transactional
    public void addOperator(LoginUser user, OperatorCreateReq req) {
        requireAdmin(user);
        if (req.getUsername() == null || req.getUsername().isBlank()) {
            throw new BusinessException("登录账号不能为空");
        }
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<EnterpriseUser>().eq(EnterpriseUser::getUsername, req.getUsername()));
        if (count != null && count > 0) {
            throw new BusinessException("登录账号已存在");
        }
        EnterpriseUser op = new EnterpriseUser();
        op.setUsername(req.getUsername().trim());
        op.setPassword(ENCODER.encode(req.getPassword() == null ? "123456" : req.getPassword()));
        op.setEnterpriseId(user.getEnterpriseId());
        op.setRealName(req.getRealName());
        op.setPhone(req.getPhone());
        op.setRole("OPERATOR");
        op.setStatus(1);
        op.setCreateTime(LocalDateTime.now());
        userMapper.insert(op);
        log.info("企业{}法人{}新增经办人: {}", user.getEnterpriseId(), user.getUserName(), op.getUsername());
    }

    /**
     * 法人删除/禁用经办人(不能删自己;不能删法人)
     */
    @Transactional
    public void disableOperator(LoginUser user, Long operatorId) {
        requireAdmin(user);
        EnterpriseUser op = userMapper.selectById(operatorId);
        if (op == null || !op.getEnterpriseId().equals(user.getEnterpriseId())) {
            throw new BusinessException("经办人不存在或无权操作");
        }
        if ("ADMIN".equals(op.getRole())) {
            throw new BusinessException("不能禁用法人账号");
        }
        if (op.getId().equals(user.getUserId())) {
            throw new BusinessException("不能禁用当前登录账号");
        }
        op.setStatus(0);
        op.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(op);
        log.info("企业{}法人{}禁用经办人: {}", user.getEnterpriseId(), user.getUserName(), op.getUsername());
    }

    /**
     * 按企业ID查法人+成员(跨服务只读,供监管端展示企业人员构成;不含密码)
     */
    public EnterpriseMemberVO membersOf(Long enterpriseId) {
        EnterpriseMemberVO vo = new EnterpriseMemberVO();
        vo.setEnterpriseId(enterpriseId);
        EnterpriseInfo info = enterpriseId == null ? null : enterpriseInfoMapper.selectById(enterpriseId);
        if (info != null) {
            vo.setLegalPerson(info.getLegalPerson());
            vo.setLegalPersonIdCard(info.getLegalPersonIdCard());
        }
        List<EnterpriseUser> users = enterpriseId == null ? List.of() : userMapper.selectList(
                new LambdaQueryWrapper<EnterpriseUser>()
                        .eq(EnterpriseUser::getEnterpriseId, enterpriseId)
                        .orderByAsc(EnterpriseUser::getRole));
        vo.setMembers(users.stream().map(u -> {
            EnterpriseMemberItemVO item = new EnterpriseMemberItemVO();
            item.setId(u.getId());
            item.setUsername(u.getUsername());
            item.setRealName(u.getRealName());
            item.setPhone(u.getPhone());
            item.setRole(u.getRole());
            item.setStatus(u.getStatus());
            return item;
        }).collect(Collectors.toList()));
        return vo;
    }

    /**
     * 列出全部企业用户(跨服只读,供监管端用户管理展示;含企业名,不含密码)
     */
    public List<EnterpriseUser> listAllWithEnterpriseName() {
        List<EnterpriseUser> users = userMapper.selectList(null);
        List<Long> entIds = users.stream()
                .map(EnterpriseUser::getEnterpriseId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> nameMap = new java.util.HashMap<>();
        if (!entIds.isEmpty()) {
            enterpriseInfoMapper.selectBatchIds(entIds)
                    .forEach(info -> nameMap.put(info.getId(), info.getEnterpriseName()));
        }
        users.forEach(u -> {
            u.setEnterpriseName(nameMap.get(u.getEnterpriseId()));
            u.setPassword(null);
        });
        return users;
    }

    /**
     * 跨服禁用经办人(监管端作为监管处置调用;不允许禁用法人账号 ADMIN)
     */
    public void disableOperatorCross(Long operatorId) {
        EnterpriseUser op = userMapper.selectById(operatorId);
        if (op == null) {
            throw new BusinessException("经办人不存在");
        }
        if ("ADMIN".equals(op.getRole())) {
            throw new BusinessException("不能禁用法人账号");
        }
        op.setStatus(0);
        op.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(op);
        log.info("监管端禁用经办人: id={}, username={}, 企业={}", operatorId, op.getUsername(), op.getEnterpriseId());
    }

    /**
     * 跨服启用经办人(监管端解除禁用,恢复其企业端登录)
     */
    public void enableOperatorCross(Long operatorId) {
        EnterpriseUser op = userMapper.selectById(operatorId);
        if (op == null) {
            throw new BusinessException("经办人不存在");
        }
        op.setStatus(1);
        op.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(op);
        log.info("监管端启用经办人: id={}, username={}, 企业={}", operatorId, op.getUsername(), op.getEnterpriseId());
    }

    private void requireAdmin(LoginUser user) {
        if (!"ADMIN".equals(user.getRole())) {
            throw new BusinessException("仅法人/管理员可管理经办人");
        }
    }
}
