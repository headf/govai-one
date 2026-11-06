package com.govai.supervision.service;

import com.govai.supervision.dto.risk.RiskRuleDTO;
import com.govai.supervision.entity.RiskRule;

import java.util.List;

/**
 * 风险预警规则服务
 */
public interface RiskRuleService {

    /** 规则列表（可按类型/状态） */
    List<RiskRule> listRules(String ruleType, Integer status);

    /** 全部启用中的规则（供定时任务扫描） */
    List<RiskRule> listEnabledRules();

    /** 新增规则 */
    RiskRule createRule(RiskRuleDTO dto);

    /** 更新规则 */
    void updateRule(RiskRuleDTO dto);

    /** 删除规则 */
    void deleteRule(Long id);
}