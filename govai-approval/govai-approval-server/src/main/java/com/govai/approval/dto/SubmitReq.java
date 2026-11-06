package com.govai.approval.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

/**
 * 提交审批请求体（接口4：POST /api/v1/approval/submit）
 * 消费方：群众端 / 企业端
 */
@Data
public class SubmitReq {
    /** 流程模板编码（与事项编码一致，如 GA-HK-001） */
    @NotBlank(message = "模板编码不能为空")
    private String templateCode;

    @NotNull(message = "事项ID不能为空")
    private Long matterId;

    @NotBlank(message = "申请人姓名不能为空")
    private String applicantName;

    @NotBlank(message = "申请人证件号不能为空")
    private String applicantId;

    /** PERSONAL / ENTERPRISE */
    @NotBlank(message = "申请人类型不能为空")
    private String applicantType;

    /** 事项名称（可选，便于审批人查看） */
    private String matterName;

    /** 申报表单数据 */
    private Map<String, Object> formData;

    /** 申报来源用户ID（群众端user_id 或企业端enterprise_user_id） */
    private Long sourceUserId;
}
