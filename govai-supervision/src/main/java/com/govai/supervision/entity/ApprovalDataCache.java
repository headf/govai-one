package com.govai.supervision.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 审批数据缓存实体（RabbitMQ 接收审批端推送，写入本表）
 */
@Data
@TableName("approval_data_cache")
public class ApprovalDataCache implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联审批实例ID（跨服务） */
    private Long instanceId;

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

    /** 申报企业ID（企业端上报，enterprise_info.id） */
    private Long enterpriseId;

    /** 申报企业名称（企业端上报冗余，避免跨端回填） */
    private String enterpriseName;

    /** 申报公民ID（群众端上报，监管端 sys_user.id；PERSONAL 办件定位公民画像） */
    private Long applicantUserId;

    /** 申报人手机号（群众端上报冗余） */
    private String applicantPhone;

    /** 申报人证件号（身份证, 群众端上报冗余） */
    private String applicantIdCard;

    private LocalDateTime receiveTime;
}