package com.govai.supervision.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.govai.supervision.common.PageResult;
import com.govai.supervision.dto.credit.CreditScoreReq;
import com.govai.supervision.entity.CreditEnterprise;
import com.govai.supervision.entity.CreditLevelUtil;
import com.govai.supervision.mapper.CreditEnterpriseMapper;
import com.govai.supervision.service.CreditEnterpriseService;
import com.govai.supervision.util.EnterpriseProfileClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 企业信用服务实现
 */
@Service
@RequiredArgsConstructor
public class CreditEnterpriseServiceImpl implements CreditEnterpriseService {

    private final CreditEnterpriseMapper creditEnterpriseMapper;
    private final EnterpriseProfileClient enterpriseProfileClient;

    @Override
    public PageResult<CreditEnterprise> pageEnterprise(long page, long size, Long enterpriseId, String level) {
        LambdaQueryWrapper<CreditEnterprise> wrapper = new LambdaQueryWrapper<CreditEnterprise>()
                .eq(enterpriseId != null, CreditEnterprise::getEnterpriseId, enterpriseId)
                .eq(level != null && !level.isBlank(), CreditEnterprise::getCreditLevel, level)
                .orderByDesc(CreditEnterprise::getUpdateTime);
        PageResult<CreditEnterprise> result = PageResult.of(creditEnterpriseMapper.selectPage(new Page<>(page, size), wrapper));
        // 跨端回填企业名称/信用代码(批量一次调用,企业端离线则静默为空)
        fillEnterpriseNames(result.getList());
        return result;
    }

    private void fillEnterpriseNames(List<CreditEnterprise> list) {
        List<Long> ids = list.stream()
                .map(CreditEnterprise::getEnterpriseId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return;
        }
        Map<Long, EnterpriseProfileClient.Brief> briefs = enterpriseProfileClient.fetchBriefs(ids);
        if (briefs.isEmpty()) {
            return;
        }
        for (CreditEnterprise e : list) {
            EnterpriseProfileClient.Brief b = briefs.get(e.getEnterpriseId());
            if (b != null) {
                e.setEnterpriseName(b.getEnterpriseName());
                e.setCreditCode(b.getCreditCode());
            }
        }
    }

    @Override
    public CreditEnterprise getByEnterpriseId(Long enterpriseId) {
        return creditEnterpriseMapper.selectOne(
                new LambdaQueryWrapper<CreditEnterprise>().eq(CreditEnterprise::getEnterpriseId, enterpriseId));
    }

    @Override
    public CreditEnterprise getByCreditCode(String creditCode) {
        if (creditCode == null || creditCode.isBlank()) {
            return null;
        }
        return creditEnterpriseMapper.selectOne(
                new LambdaQueryWrapper<CreditEnterprise>().eq(CreditEnterprise::getCreditCode, creditCode));
    }

    @Override
    public CreditEnterprise saveOrUpdateScore(CreditScoreReq req) {
        CreditEnterprise exist = getByEnterpriseId(req.getTargetId());
        if (exist == null) {
            exist = new CreditEnterprise();
            exist.setEnterpriseId(req.getTargetId());
        }
        exist.setId(exist.getId());
        exist.setCreditScore(req.getCreditScore());
        exist.setCreditLevel(CreditLevelUtil.levelOf(req.getCreditScore()));
        exist.setDimensionScores(req.getDimensionScores());
        if (exist.getId() == null) {
            creditEnterpriseMapper.insert(exist);
        } else {
            creditEnterpriseMapper.updateById(exist);
        }
        return exist;
    }

    @Override
    public BigDecimal adjustScore(Long enterpriseId, BigDecimal newScore) {
        CreditEnterprise exist = getByEnterpriseId(enterpriseId);
        if (exist == null) {
            exist = new CreditEnterprise();
            exist.setEnterpriseId(enterpriseId);
        }
        exist.setCreditScore(newScore);
        exist.setCreditLevel(CreditLevelUtil.levelOf(newScore));
        if (exist.getId() == null) {
            creditEnterpriseMapper.insert(exist);
        } else {
            creditEnterpriseMapper.updateById(exist);
        }
        return newScore;
    }

    @Override
    public BigDecimal addScoreBonus(Long enterpriseId, BigDecimal bonus) {
        CreditEnterprise exist = getByEnterpriseId(enterpriseId);
        BigDecimal base = exist != null && exist.getCreditScore() != null ? exist.getCreditScore() : BigDecimal.ZERO;
        BigDecimal newScore = base.add(bonus == null ? BigDecimal.ZERO : bonus);
        if (newScore.compareTo(BigDecimal.valueOf(100)) > 0) {
            newScore = BigDecimal.valueOf(100);
        }
        if (newScore.compareTo(BigDecimal.ZERO) < 0) {
            newScore = BigDecimal.ZERO;
        }
        return adjustScore(enterpriseId, newScore);
    }

    @Override
    public void deleteByEnterpriseId(Long enterpriseId) {
        creditEnterpriseMapper.delete(
                new LambdaQueryWrapper<CreditEnterprise>().eq(CreditEnterprise::getEnterpriseId, enterpriseId));
    }
}