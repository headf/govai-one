package com.govai.approval.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 审批实例表
 */
@Data
@TableName("approval_instance")
public class ApprovalInstance implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long templateId;
    private Long matterId;
    private String matterName;
    private String applicantName;
    private String applicantId;
    /** PERSONAL/ENTERPRISE */
    private String applicantType;
    /** 申报表单数据(JSON) */
    private String formData;
    /** PENDING/APPROVING/APPROVED/REJECTED/RETURNED */
    private String status;
    /** MANUAL人工流转/AUTO即办件·系统自动办结 */
    private String settleMode;
    private LocalDateTime submitTime;
    private Long currentNodeId;
    private LocalDateTime finishTime;
    private LocalDateTime createTime;

    /** 企业信用等级 A/B/C/D(审批展示用,非表字段,详情时跨端查监管端填充) */
    @TableField(exist = false)
    private String creditLevel;

    /** 企业信用分(审批展示用,非表字段) */
    @TableField(exist = false)
    private BigDecimal creditScore;
}
