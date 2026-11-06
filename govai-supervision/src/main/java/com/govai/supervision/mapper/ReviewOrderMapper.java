package com.govai.supervision.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.govai.supervision.entity.ReviewOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 差评整改工单 Mapper
 */
@Mapper
public interface ReviewOrderMapper extends BaseMapper<ReviewOrder> {
}
