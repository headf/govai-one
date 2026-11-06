package com.govai.supervision.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.govai.supervision.entity.ApprovalDataCache;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审批数据缓存 Mapper
 */
@Mapper
public interface ApprovalDataCacheMapper extends BaseMapper<ApprovalDataCache> {
}