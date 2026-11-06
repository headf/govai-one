package com.govai.supervision.dto.sample;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 定向抽样请求
 */
@Data
@Schema(description = "定向抽样请求")
public class TargetedSampleReq implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "审批实例ID不能为空")
    @Schema(description = "审批实例ID")
    private Long instanceId;

    @NotBlank(message = "检查结果不能为空")
    @Schema(description = "检查结果：PASS/FAIL")
    private String checkResult;

    @Schema(description = "检查详情")
    private String checkDetail;
}