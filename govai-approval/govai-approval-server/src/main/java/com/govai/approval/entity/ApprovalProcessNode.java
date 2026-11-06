package com.govai.approval.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 流程环节表
 */
@Data
@TableName("approval_process_node")
public class ApprovalProcessNode implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long templateId;
    private String nodeName;
    /** START/APPROVAL/RULE_CHECK/BRANCH/END */
    private String nodeType;
    /** 办理人角色 APPROVER/SUPERVISOR */
    private String assigneeRole;
    private Integer sortOrder;
    private Long nextNodeId;
    /** 分支配置(JSON) */
    private String branchConfig;
}
