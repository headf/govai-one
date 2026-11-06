package com.govai.supervision.service;

import com.govai.supervision.common.PageResult;
import com.govai.supervision.entity.AuditTrace;

/**
 * 审计追溯服务
 */
public interface AuditTraceService {

    /** 记录一条审计（写入全链路 JSON） */
    void record(Long instanceId, String traceData);

    /** 分页查询审计记录 */
    PageResult<AuditTrace> pageTraces(long page, long size, Long instanceId);

    /** 按实例号回溯整条链路（聚合审批缓存/抽样/预警数据为 JSON 串返回） */
    String traceChain(Long instanceId);
}