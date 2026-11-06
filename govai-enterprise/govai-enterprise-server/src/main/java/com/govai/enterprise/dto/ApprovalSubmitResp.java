package com.govai.enterprise.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 提交审批响应(审批端返回)
 */
@Data
@Schema(description = "提交审批响应")
public class ApprovalSubmitResp {

    @Schema(description = "审批实例ID")
    private Long instanceId;

    @Schema(description = "状态 APPROVING/APPROVED...")
    private String status;

    @Schema(description = "是否本地模拟(审批端不可用时的降级结果)")
    private Boolean mock;
}
