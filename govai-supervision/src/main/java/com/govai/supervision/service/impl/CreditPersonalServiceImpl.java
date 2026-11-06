package com.govai.supervision.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.govai.supervision.common.BizNoGenerator;
import com.govai.supervision.common.PageResult;
import com.govai.supervision.dto.credit.CreditScoreReq;
import com.govai.supervision.entity.CreditLevelUtil;
import com.govai.supervision.entity.CreditPersonal;
import com.govai.supervision.entity.SysUser;
import com.govai.supervision.mapper.CreditPersonalMapper;
import com.govai.supervision.mapper.SysUserMapper;
import com.govai.supervision.service.CreditPersonalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 个人信用服务实现
 */
@Service
@RequiredArgsConstructor
public class CreditPersonalServiceImpl implements CreditPersonalService {

    private final CreditPersonalMapper creditPersonalMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    public PageResult<CreditPersonal> pagePersonal(long page, long size, Long userId, String level) {
        LambdaQueryWrapper<CreditPersonal> wrapper = new LambdaQueryWrapper<CreditPersonal>()
                .eq(userId != null, CreditPersonal::getUserId, userId)
                .eq(level != null && !level.isBlank(), CreditPersonal::getCreditLevel, level)
                .orderByDesc(CreditPersonal::getUpdateTime);
        PageResult<CreditPersonal> result = PageResult.of(creditPersonalMapper.selectPage(new Page<>(page, size), wrapper));
        fillUserNames(result.getList());
        return result;
    }

    @Override
    public CreditPersonal getByUserId(Long userId) {
        return creditPersonalMapper.selectOne(
                new LambdaQueryWrapper<CreditPersonal>().eq(CreditPersonal::getUserId, userId));
    }

    /** 回填用户姓名(查 sys_user.real_name),供前端展示替代裸用户ID */
    private void fillUserNames(java.util.List<CreditPersonal> list) {
        java.util.List<Long> ids = list.stream()
                .map(CreditPersonal::getUserId)
                .filter(id -> id != null)
                .distinct()
                .collect(java.util.stream.Collectors.toList());
        if (ids.isEmpty()) {
            return;
        }
        java.util.List<SysUser> users = sysUserMapper.selectBatchIds(ids);
        java.util.Map<Long, String> nameMap = users.stream()
                .filter(u -> u.getId() != null)
                .collect(java.util.stream.Collectors.toMap(SysUser::getId,
                        u -> u.getRealName() != null && !u.getRealName().isBlank() ? u.getRealName() : u.getUsername(),
                        (a, b) -> a));
        for (CreditPersonal p : list) {
            if (p.getUserId() != null) {
                p.setUserName(nameMap.get(p.getUserId()));
            }
        }
    }

    @Override
    public CreditPersonal saveOrUpdateScore(CreditScoreReq req) {
        CreditPersonal exist = getByUserId(req.getTargetId());
        if (exist == null) {
            exist = new CreditPersonal();
            exist.setCreditNo(BizNoGenerator.gen("CX"));
            exist.setUserId(req.getTargetId());
        }
        exist.setCreditScore(req.getCreditScore());
        exist.setCreditLevel(CreditLevelUtil.levelOf(req.getCreditScore()));
        if (exist.getId() == null) {
            creditPersonalMapper.insert(exist);
        } else {
            creditPersonalMapper.updateById(exist);
        }
        return exist;
    }

    @Override
    public BigDecimal addScoreBonus(Long userId, BigDecimal bonus) {
        CreditPersonal exist = getByUserId(userId);
        BigDecimal base = exist != null && exist.getCreditScore() != null ? exist.getCreditScore() : BigDecimal.ZERO;
        BigDecimal newScore = base.add(bonus == null ? BigDecimal.ZERO : bonus);
        if (newScore.compareTo(BigDecimal.valueOf(100)) > 0) {
            newScore = BigDecimal.valueOf(100);
        }
        if (newScore.compareTo(BigDecimal.ZERO) < 0) {
            newScore = BigDecimal.ZERO;
        }
        CreditPersonal score = new CreditPersonal();
        score.setUserId(userId);
        score.setCreditScore(newScore);
        score.setCreditLevel(CreditLevelUtil.levelOf(newScore));
        if (exist == null) {
            score.setCreditNo(BizNoGenerator.gen("CX"));
            score.setId(null);
            creditPersonalMapper.insert(score);
        } else {
            score.setId(exist.getId());
            creditPersonalMapper.updateById(score);
        }
        return newScore;
    }

    @Override
    public void deleteByUserId(Long userId) {
        creditPersonalMapper.delete(
                new LambdaQueryWrapper<CreditPersonal>().eq(CreditPersonal::getUserId, userId));
    }
}