package com.govai.approval.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 提交审批响应体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmitVO {
    /** 审批实例ID */
    private Long instanceId;
    /** 状态：PENDING/APPROVING/APPROVED/REJECTED/RETURNED */
    private String status;
    /** 当前环节名称 */
    private String currentNodeName;
    /** 规则引擎自动审核结果（若启用） */
    private String ruleResult;
}
