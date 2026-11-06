package com.govai.supervision.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.govai.supervision.entity.StatsDaily;
import org.apache.ibatis.annotations.Mapper;

/**
 * 日统计汇总 Mapper
 */
@Mapper
public interface StatsDailyMapper extends BaseMapper<StatsDaily> {
}