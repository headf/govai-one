package com.govai.supervision.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.govai.supervision.entity.RiskRule;
import org.apache.ibatis.annotations.Mapper;

/**
 * 风险预警规则 Mapper
 */
@Mapper
public interface RiskRuleMapper extends BaseMapper<RiskRule> {
}