package com.govai.enterprise.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 审批进度查询响应(契约接口5: GET /api/v1/approval/progress/{id},审批端返回)
 */
@Data
@Schema(description = "审批进度")
public class ApprovalProgressResp {

    @Schema(description = "审批实例ID")
    private Long instanceId;

    @Schema(description = "状态 PENDING/APPROVING/APPROVED/REJECTED")
    private String status;

    @Schema(description = "MANUAL人工/AUTO即办件·系统自动办结")
    private String settleMode;

    @Schema(description = "当前环节名称")
    private String currentNode;

    @Schema(description = "提交时间")
    private String submitTime;

    @Schema(description = "环节明细(名称/状态/办理人/岗位/结果/意见/证照号)")
    private List<Node> nodes;

    @Data
    public static class Node {
        @Schema(description = "环节名称")
        private String name;
        @Schema(description = "状态 DONE/CURRENT/PENDING")
        private String status;
        @Schema(description = "环节岗位码 APPROVER/DECIDE/ISSUE")
        private String assigneeRole;
        @Schema(description = "办理人")
        private String assignee;
        @Schema(description = "办理时间")
        private String handleTime;
        @Schema(description = "审批结果 PASS/REJECT/RETURN")
        private String approveResult;
        @Schema(description = "审批意见")
        private String approveOpinion;
        @Schema(description = "办理附加信息(JSON):办结出证含送达方式/证照号 licenseNo")
        private String handleDetail;
    }
}
