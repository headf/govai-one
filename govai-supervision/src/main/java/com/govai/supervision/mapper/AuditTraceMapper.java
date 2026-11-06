package com.govai.supervision.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.govai.supervision.entity.AuditTrace;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审计追溯 Mapper
 */
@Mapper
public interface AuditTraceMapper extends BaseMapper<AuditTrace> {
}