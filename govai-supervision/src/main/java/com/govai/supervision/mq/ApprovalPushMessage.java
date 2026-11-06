package com.govai.supervision.mq;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 审批端通过 RabbitMQ 推送的审批结果消息体（契约 9，与审批端 JSON 字段对齐）
 * 消费者收到后写入 approval_data_cache 表
 */
@Data
public class ApprovalPushMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long instanceId;

    /** 申报企业ID（企业端上报，enterprise_info.id；审批端推送可空） */
    private Long enterpriseId;

    /** 申报企业名称（冗余展示） */
    private String enterpriseName;

    private String matterName;

    private String applicantName;

    /** 申请人类型：PERSONAL/ENTERPRISE */
    private String applicantType;

    /** 审批状态：PENDING/APPROVED/REJECTED 等 */
    private String approvalStatus;

    private LocalDateTime submitTime;

    private LocalDateTime finishTime;

    /** 办理总时长（小时） */
    private BigDecimal processDurationHours;

    /** 申报公民ID（群众端上报, 指向监管端 sys_user.id；审批端 MQ 推送可空） */
    private Long applicantUserId;

    /** 申报人手机号（群众端上报补全） */
    private String applicantPhone;

    /** 申报人证件号（身份证, 群众端上报补全） */
    private String applicantIdCard;
}