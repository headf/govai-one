package com.govai.supervision.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.govai.supervision.entity.SysLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统操作日志 Mapper
 */
@Mapper
public interface SysLogMapper extends BaseMapper<SysLog> {
}
