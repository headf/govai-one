package com.govai.approval.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.govai.approval.common.PageResult;
import com.govai.approval.entity.AuditLog;
import com.govai.approval.mapper.AuditLogMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 审计日志查询 Service
 *
 * 需求5.3：审批全流程操作留痕是政务系统的合规底线
 * 留痕数据不可修改，仅供审计查询
 * 支持按 事项类型 / 办理人 / 时间段 / 办理结果 四个维度筛选
 */
@Service
public class AuditLogService extends ServiceImpl<AuditLogMapper, AuditLog> {

    public PageResult<AuditLog> page(Long instanceId, Long operatorId, String operationType,
                                     LocalDateTime from, LocalDateTime to, int page, int size) {
        LambdaQueryWrapper<AuditLog> w = new LambdaQueryWrapper<>();
        if (instanceId != null) w.eq(AuditLog::getInstanceId, instanceId);
        if (operatorId != null) w.eq(AuditLog::getOperatorId, operatorId);
        if (StringUtils.hasText(operationType)) w.eq(AuditLog::getOperationType, operationType);
        if (from != null) w.ge(AuditLog::getOperationTime, from);
        if (to != null) w.le(AuditLog::getOperationTime, to);
        w.orderByDesc(AuditLog::getOperationTime);
        Page<AuditLog> p = page(new Page<>(page, size), w);
        return PageResult.of(p.getRecords(), p.getTotal(), page, size);
    }
}
