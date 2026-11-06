package com.govai.enterprise.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 企业申报上报监管端请求
 * 字段与监管端 ApprovalPushMessage 对齐(instance_id 去重维度为 instanceId)。
 */
@Data
public class SupervisionReportReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 审批实例ID(监管端按此去重);企业端演示无真实实例时用申报记录ID */
    private Long instanceId;

    /** 申报企业ID(enterprise_info.id) */
    private Long enterpriseId;

    /** 申报企业名称 */
    private String enterpriseName;

    private String matterName;

    /** 申请人名称(上报=企业名称) */
    private String applicantName;

    /** PENDING/APPROVED/REJECTED(监管端状态口径) */
    private String approvalStatus;

    private LocalDateTime submitTime;

    private LocalDateTime finishTime;

    /** 办理时长(小时) */
    private BigDecimal processDurationHours;
}
