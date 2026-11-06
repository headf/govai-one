package com.govai.supervision.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.govai.supervision.entity.SupervisionSample;
import org.apache.ibatis.annotations.Mapper;

/**
 * 抽样检查 Mapper
 */
@Mapper
public interface SupervisionSampleMapper extends BaseMapper<SupervisionSample> {
}