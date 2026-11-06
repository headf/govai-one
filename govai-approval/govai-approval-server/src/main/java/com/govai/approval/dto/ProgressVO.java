package com.govai.approval.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审批进度响应体（接口5：GET /api/v1/approval/progress/{instanceId}）
 */
@Data
public class ProgressVO {
    private Long instanceId;
    /** 实例状态 PENDING/APPROVING/APPROVED/REJECTED/RETURNED */
    private String status;
    /** MANUAL人工/AUTO即办件·系统自动办结 */
    private String settleMode;
    private String currentNode;
    private LocalDateTime submitTime;
    private LocalDateTime finishTime;
    /** 进度节点列表 */
    private List<NodeProgress> nodes;

    @Data
    public static class NodeProgress {
        /** 环节对应的任务ID(有任务时返回,供审批操作定位待办) */
        private Long taskId;
        private String name;
        /** DONE/CURRENT/PENDING */
        private String status;
        /** 环节岗位码(快照) APPROVER/DECIDE/ISSUE */
        private String assigneeRole;
        private String assignee;
        private LocalDateTime handleTime;
        private String approveResult;
        private String approveOpinion;
        /** 办理附加信息(JSON):办结出证含送达方式/企业端回传证照号 licenseNo */
        private String handleDetail;
        /** 当前登录人是否为该环节待办任务的操作岗(供前端按岗位渲染操作区) */
        private Boolean canOperate;
    }
}
