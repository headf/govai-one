package com.govai.supervision.service.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.govai.supervision.common.BusinessException;
import com.govai.supervision.common.PageResult;
import com.govai.supervision.entity.ApprovalDataCache;
import com.govai.supervision.entity.AuditTrace;
import com.govai.supervision.entity.RiskAlert;
import com.govai.supervision.entity.SupervisionSample;
import com.govai.supervision.mapper.ApprovalDataCacheMapper;
import com.govai.supervision.mapper.AuditTraceMapper;
import com.govai.supervision.mapper.RiskAlertMapper;
import com.govai.supervision.mapper.SupervisionSampleMapper;
import com.govai.supervision.service.AuditTraceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审计追溯服务实现
 */
@Service
@RequiredArgsConstructor
public class AuditTraceServiceImpl implements AuditTraceService {

    private final AuditTraceMapper auditTraceMapper;
    private final ApprovalDataCacheMapper approvalDataCacheMapper;
    private final SupervisionSampleMapper supervisionSampleMapper;
    private final RiskAlertMapper riskAlertMapper;

    @Override
    public void record(Long instanceId, String traceData) {
        AuditTrace trace = new AuditTrace();
        trace.setInstanceId(instanceId);
        trace.setTraceData(traceData);
        trace.setCreateTime(LocalDateTime.now());
        auditTraceMapper.insert(trace);
    }

    @Override
    public PageResult<AuditTrace> pageTraces(long page, long size, Long instanceId) {
        LambdaQueryWrapper<AuditTrace> wrapper = new LambdaQueryWrapper<AuditTrace>()
                .eq(instanceId != null, AuditTrace::getInstanceId, instanceId)
                .orderByDesc(AuditTrace::getCreateTime);
        return PageResult.of(auditTraceMapper.selectPage(new Page<>(page, size), wrapper));
    }

    @Override
    public String traceChain(Long instanceId) {
        JSONObject chain = new JSONObject();
        chain.set("instanceId", instanceId);

        // 1. 审批缓存
        ApprovalDataCache approval = approvalDataCacheMapper.selectOne(
                new LambdaQueryWrapper<ApprovalDataCache>().eq(ApprovalDataCache::getInstanceId, instanceId));
        chain.set("approvalRecord", approval != null ? JSONUtil.parseObj(approval) : null);

        // 2. 抽样检查
        List<SupervisionSample> samples = supervisionSampleMapper.selectList(
                new LambdaQueryWrapper<SupervisionSample>().eq(SupervisionSample::getInstanceId, instanceId));
        chain.set("samples", samples);

        // 3. 风险预警
        List<RiskAlert> alerts = riskAlertMapper.selectList(
                new LambdaQueryWrapper<RiskAlert>().eq(RiskAlert::getTargetId, instanceId));
        chain.set("riskAlerts", alerts);

        // 4. 审计记录
        List<AuditTrace> audits = auditTraceMapper.selectList(
                new LambdaQueryWrapper<AuditTrace>().eq(AuditTrace::getInstanceId, instanceId));
        chain.set("auditTraces", audits);

        return chain.toString();
    }
}