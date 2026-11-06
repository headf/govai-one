package com.govai.supervision.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.govai.supervision.common.BusinessException;
import com.govai.supervision.common.PageResult;
import com.govai.supervision.entity.RiskAlert;
import com.govai.supervision.mapper.RiskAlertMapper;
import com.govai.supervision.service.RiskAlertService;
import com.govai.supervision.service.SupervisionSampleService;
import com.govai.supervision.util.EnterpriseProfileClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 风险预警记录服务实现
 */
@Service
@RequiredArgsConstructor
public class RiskAlertServiceImpl implements RiskAlertService {

    private final RiskAlertMapper riskAlertMapper;
    private final EnterpriseProfileClient enterpriseProfileClient;
    private final SupervisionSampleService supervisionSampleService;

    @Override
    public PageResult<RiskAlert> pageAlerts(long page, long size, String level, String handleStatus, String targetType) {
        LambdaQueryWrapper<RiskAlert> wrapper = new LambdaQueryWrapper<RiskAlert>()
                .eq(StringUtils.hasText(level), RiskAlert::getAlertLevel, level)
                .eq(StringUtils.hasText(handleStatus), RiskAlert::getHandleStatus, handleStatus)
                .eq(StringUtils.hasText(targetType), RiskAlert::getTargetType, targetType)
                .orderByDesc(RiskAlert::getAlertTime);
        PageResult<RiskAlert> result = PageResult.of(riskAlertMapper.selectPage(new Page<>(page, size), wrapper));
        fillEnterpriseNames(result.getList());
        return result;
    }

    @Override
    public void dispatch(Long id, Long checkerId) {
        RiskAlert alert = riskAlertMapper.selectById(id);
        if (alert == null) {
            throw new BusinessException("预警不存在");
        }
        if (!"PENDING".equals(alert.getHandleStatus())) {
            throw new BusinessException("仅未处置的预警可转定向抽查");
        }
        if (alert.getTargetId() == null) {
            throw new BusinessException("该预警无关联办件，无法转定向抽查");
        }
        String detail = "由预警(" + alert.getAlertNo() + ")自动转定向抽查："
                + (alert.getAlertContent() == null ? alert.getTargetType() : alert.getAlertContent());
        supervisionSampleService.targetedSample(alert.getTargetId(), "PENDING", detail, checkerId);
        alert.setHandleStatus("HANDLED");
        alert.setHandleOpinion("已转定向抽查");
        alert.setHandlerId(checkerId);
        alert.setHandleTime(LocalDateTime.now());
        riskAlertMapper.updateById(alert);
    }

    /** 对企业类预警回填企业名称 */
    private void fillEnterpriseNames(List<RiskAlert> list) {
        List<Long> ids = list.stream()
                .filter(a -> "ENTERPRISE".equals(a.getTargetType()) && a.getTargetId() != null)
                .map(RiskAlert::getTargetId)
                .distinct()
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return;
        }
        Map<Long, EnterpriseProfileClient.Brief> briefs = enterpriseProfileClient.fetchBriefs(ids);
        if (briefs.isEmpty()) {
            return;
        }
        for (RiskAlert a : list) {
            if ("ENTERPRISE".equals(a.getTargetType())) {
                EnterpriseProfileClient.Brief b = briefs.get(a.getTargetId());
                if (b != null) {
                    a.setEnterpriseName(b.getEnterpriseName());
                }
            }
        }
    }

    @Override
    public long countTotal() {
        return riskAlertMapper.selectCount(null);
    }

    @Override
    public long countPending() {
        return riskAlertMapper.selectCount(new LambdaQueryWrapper<RiskAlert>()
                .eq(RiskAlert::getHandleStatus, "PENDING"));
    }

    @Override
    public void handle(Long id, String handleOpinion, Long handlerId) {
        RiskAlert alert = riskAlertMapper.selectById(id);
        if (alert == null) {
            throw new BusinessException(404, "预警记录不存在");
        }
        if ("HANDLED".equals(alert.getHandleStatus())) {
            throw new BusinessException(400, "该预警已处理");
        }
        RiskAlert update = new RiskAlert();
        update.setId(id);
        update.setHandleStatus("HANDLED");
        update.setHandleOpinion(handleOpinion);
        update.setHandlerId(handlerId);
        update.setHandleTime(LocalDateTime.now());
        riskAlertMapper.updateById(update);
    }
}