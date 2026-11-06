package com.govai.approval.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 审批数据推送DTO（接口9：通过 RabbitMQ 推送到监管端）
 * 监管端消费后写入 approval_data_cache 表
 */
@Data
public class ApprovalRecordPushDTO implements Serializable {
    private Long instanceId;
    private String matterName;
    private String applicantName;
    /** PERSONAL / ENTERPRISE */
    private String applicantType;
    /** APPROVED / REJECTED / RETURNED */
    private String approvalStatus;
    private LocalDateTime submitTime;
    private LocalDateTime finishTime;
    private Double processDurationHours;
}
