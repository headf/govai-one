package com.govai.supervision.service;

import com.govai.supervision.common.PageResult;
import com.govai.supervision.entity.SysLog;

/**
 * 系统操作日志服务
 */
public interface SysLogService {

    /** 分页查询（可按操作人/操作/时间过滤） */
    PageResult<SysLog> pageLogs(long page, long size, String keyword, String begin, String end);

    /** 写入一条日志（供切面调用） */
    void record(SysLog log);

    /** 清空日志（仅 admin） */
    void clearAll();
}
