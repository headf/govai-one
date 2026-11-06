package com.govai.approval.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 审计日志表（不可修改、仅供审计查询）
 */
@Data
@TableName("audit_log")
public class AuditLog implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long instanceId;
    private Long operatorId;
    private String operatorName;
    /** SUBMIT/RECEIVE/APPROVE/RETURN/FINISH/AI_ADVICE */
    private String operationType;
    private String operationOpinion;
    private String beforeStatus;
    private String afterStatus;
    private LocalDateTime operationTime;
}
