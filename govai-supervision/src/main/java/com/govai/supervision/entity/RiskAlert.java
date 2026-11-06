package com.govai.supervision.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 风险预警记录实体
 */
@Data
@TableName("risk_alert")
public class RiskAlert implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 预警编号(YJ+日期+随机),供前端展示替代裸自增id */
    private String alertNo;

    /** 触发的规则ID */
    private Long ruleId;

    /** 目标类型：ENTERPRISE/PERSONAL/INSTANCE */
    private String targetType;

    /** 目标ID */
    private Long targetId;

    /** 预警级别：LOW/MEDIUM/HIGH */
    private String alertLevel;

    /** 预警内容 */
    private String alertContent;

    private LocalDateTime alertTime;

    /** 处理状态：PENDING/HANDLED */
    private String handleStatus;

    /** 处置意见 */
    private String handleOpinion;

    /** 处置人ID */
    private Long handlerId;

    /** 目标企业名称（非表字段，跨端回填；仅 ENTERPRISE 类有值） */
    @TableField(exist = false)
    private String enterpriseName;

    private LocalDateTime handleTime;
}