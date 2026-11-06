package com.govai.enterprise.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

/**
 * 提交审批请求(契约接口4: POST /api/v1/approval/submit,调审批端)
 * 字段与骨架文档 5.5 保持一致
 */
@Data
@Schema(description = "提交审批请求")
public class ApprovalSubmitReq {

    @Schema(description = "流程模板编码(=事项编码 templateCode)")
    private String templateCode;

    @Schema(description = "事项ID(企业端 ent_matter_info.id)")
    private Long matterId;

    @Schema(description = "申请人姓名(经办人)")
    private String applicantName;

    @Schema(description = "申请人标识(统一社会信用代码,代表企业主体)")
    private String applicantId;

    @Schema(description = "申请人类型 ENTERPRISE")
    private String applicantType;

    @Schema(description = "表单数据(JSON对象)")
    private Map<String, Object> formData;
}
