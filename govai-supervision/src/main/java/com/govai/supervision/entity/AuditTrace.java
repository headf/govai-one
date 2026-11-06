package com.govai.supervision.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 审计追溯记录实体
 */
@Data
@TableName("audit_trace")
public class AuditTrace implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联审批实例ID */
    private Long instanceId;

    /** 全链路数据(JSON) */
    private String traceData;

    private LocalDateTime createTime;
}