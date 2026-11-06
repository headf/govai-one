package com.govai.supervision.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.govai.supervision.entity.RiskAlert;
import org.apache.ibatis.annotations.Mapper;

/**
 * 风险预警记录 Mapper
 */
@Mapper
public interface RiskAlertMapper extends BaseMapper<RiskAlert> {
}