package com.govai.supervision.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 日统计汇总实体（映射 stats_daily 表，供统计大屏时序展示）
 */
@Data
@TableName("stats_daily")
public class StatsDaily implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private LocalDate statDate;

    /** 当日办件总量 */
    private Integer totalApplications;

    /** 当日已办结(PASS/APPROVED)量 */
    private Integer approvedCount;

    /** 当日驳回(REJECTED)量 */
    private Integer rejectedCount;

    /** 当日平均办理时长(小时) */
    private BigDecimal avgProcessHours;

    /** 当日超时量 */
    private Integer timeoutCount;

    /** 当日平均满意度(1-5) */
    private BigDecimal satisfactionAvg;

    private LocalDateTime createTime;
}