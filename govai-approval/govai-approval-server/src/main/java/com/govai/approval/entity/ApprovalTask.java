package com.govai.approval.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 审批任务表
 */
@Data
@TableName("approval_task")
public class ApprovalTask implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long instanceId;
    private Long nodeId;
    /** 环节名称(冗余展示用,非表字段,查询时填充:受理/初审/决定/办结) */
    @TableField(exist = false)
    private String nodeName;
    /** 任务所属环节岗位快照(创建时按节点写入,鉴权/待办过滤用) APPROVER/DECIDE/ISSUE */
    private String processRoleCode;
    private Long assigneeUserId;
    private String assigneeUserName;
    /** PENDING/DONE/CANCEL */
    private String taskStatus;
    /** PASS/REJECT/RETURN */
    private String approveResult;
    private String approveOpinion;
    /** 办理附加信息(JSON):办结岗填写证照/批文号、送达方式等 */
    private String handleDetail;
    private LocalDateTime createTime;
    private LocalDateTime handleTime;
    /** 委托给谁 */
    private Long delegateTo;
}
