package com.govai.supervision.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 信用修复申请实体
 */
@Data
@TableName("credit_repair")
public class CreditRepair implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 修复单号(XF+日期+随机),供前端展示替代裸自增id */
    private String repairNo;

    /** 目标类型：ENTERPRISE/PERSONAL */
    private String targetType;

    /** 目标ID（企业ID或用户ID） */
    private Long targetId;

    /** 主体名称（非表字段，分页时按主体类型回填企业名/账号，供前端展示） */
    @TableField(exist = false)
    private String targetName;

    /** 修复理由 */
    private String repairReason;

    private LocalDateTime applyTime;

    /** 审核状态：PENDING/PASS/REJECT */
    private String auditStatus;

    /** 审核意见 */
    private String auditOpinion;

    private LocalDateTime auditTime;
}