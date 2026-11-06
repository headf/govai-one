package com.govai.supervision.dto.credit;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 信用修复申请请求
 */
@Data
@Schema(description = "信用修复申请")
public class CreditRepairReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 目标类型：ENTERPRISE/PERSONAL */
    @NotBlank(message = "类型不能为空")
    @Schema(description = "目标类型：ENTERPRISE/PERSONAL", example = "ENTERPRISE")
    private String targetType;

    @NotNull(message = "目标ID不能为空")
    @Schema(description = "目标ID（企业ID或用户ID）")
    private Long targetId;

    @NotBlank(message = "修复理由不能为空")
    @Schema(description = "修复理由")
    private String repairReason;
}