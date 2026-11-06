package com.govai.supervision.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 风险预警规则实体
 */
@Data
@TableName("risk_rule")
public class RiskRule implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 规则名称 */
    private String ruleName;

    /** 规则类型：TIMEOUT/ANOMALY/THRESHOLD */
    private String ruleType;

    /** 阈值（如超时小时数/申请量等，按类型解释） */
    private String thresholdValue;

    /** 预警级别：LOW/MEDIUM/HIGH */
    private String alertLevel;

    /** 通知方式：SMS/EMAIL/SITE */
    private String notifyType;

    /** 状态：1启用 0停用 */
    private Integer status;

    private LocalDateTime createTime;
}