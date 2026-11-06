package com.govai.supervision.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.govai.supervision.common.BusinessException;
import com.govai.supervision.dto.risk.RiskRuleDTO;
import com.govai.supervision.entity.RiskRule;
import com.govai.supervision.mapper.RiskRuleMapper;
import com.govai.supervision.service.RiskRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 风险预警规则服务实现
 */
@Service
@RequiredArgsConstructor
public class RiskRuleServiceImpl implements RiskRuleService {

    private final RiskRuleMapper riskRuleMapper;

    @Override
    public List<RiskRule> listRules(String ruleType, Integer status) {
        return riskRuleMapper.selectList(new LambdaQueryWrapper<RiskRule>()
                .eq(StringUtils.hasText(ruleType), RiskRule::getRuleType, ruleType)
                .eq(status != null, RiskRule::getStatus, status)
                .orderByAsc(RiskRule::getId));
    }

    @Override
    public List<RiskRule> listEnabledRules() {
        return riskRuleMapper.selectList(new LambdaQueryWrapper<RiskRule>()
                .eq(RiskRule::getStatus, 1)
                .orderByAsc(RiskRule::getId));
    }

    @Override
    public RiskRule createRule(RiskRuleDTO dto) {
        RiskRule rule = new RiskRule();
        copyFromDto(rule, dto);
        rule.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        riskRuleMapper.insert(rule);
        return rule;
    }

    @Override
    public void updateRule(RiskRuleDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(400, "规则ID不能为空");
        }
        if (riskRuleMapper.selectById(dto.getId()) == null) {
            throw new BusinessException(404, "规则不存在");
        }
        RiskRule rule = new RiskRule();
        rule.setId(dto.getId());
        copyFromDto(rule, dto);
        riskRuleMapper.updateById(rule);
    }

    @Override
    public void deleteRule(Long id) {
        if (riskRuleMapper.selectById(id) == null) {
            throw new BusinessException(404, "规则不存在");
        }
        riskRuleMapper.deleteById(id);
    }

    private void copyFromDto(RiskRule rule, RiskRuleDTO dto) {
        rule.setRuleName(dto.getRuleName());
        rule.setRuleType(dto.getRuleType());
        rule.setThresholdValue(dto.getThresholdValue());
        rule.setAlertLevel(dto.getAlertLevel());
        rule.setNotifyType(dto.getNotifyType());
        if (dto.getStatus() != null) {
            rule.setStatus(dto.getStatus());
        }
    }
}