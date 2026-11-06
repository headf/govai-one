package com.govai.supervision.service;

import com.govai.supervision.common.PageResult;
import com.govai.supervision.entity.RiskAlert;

/**
 * 风险预警记录服务
 */
public interface RiskAlertService {

    /** 预警分页查询（可按级别/状态/类型） */
    PageResult<RiskAlert> pageAlerts(long page, long size, String level, String handleStatus, String targetType);

    /** 预警总数/未处理数（大屏统计用） */
    long countTotal();

    /** 未处理预警数 */
    long countPending();

    /** 处置预警（记录意见与处置人） */
    void handle(Long id, String handleOpinion, Long handlerId);

    /** 预警转定向抽查: 自动生成 TARGETED 抽查并处置预警(无感监管自动分派) */
    void dispatch(Long id, Long checkerId);
}