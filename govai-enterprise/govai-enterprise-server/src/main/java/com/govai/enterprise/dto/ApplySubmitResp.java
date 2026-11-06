package com.govai.enterprise.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 申报提交响应
 */
@Data
@Schema(description = "申报提交响应")
public class ApplySubmitResp {

    @Schema(description = "申报记录ID")
    private Long recordId;

    @Schema(description = "申报编号")
    private String recordCode;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "审批实例ID(跨服务,若审批端在线)")
    private Long approvalInstanceId;

    @Schema(description = "是否本地模拟提交(审批端不可用时的降级结果,前端可据此提示)")
    private Boolean mocked;
}
