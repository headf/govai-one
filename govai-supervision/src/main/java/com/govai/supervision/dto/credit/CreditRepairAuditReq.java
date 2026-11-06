package com.govai.supervision.dto.credit;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 信用修复审核请求
 */
@Data
@Schema(description = "信用修复审核")
public class CreditRepairAuditReq implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "申请ID不能为空")
    @Schema(description = "申请ID")
    private Long id;

    /** 审核结论：PASS/REJECT */
    @NotNull(message = "审核结论不能为空")
    @Schema(description = "审核结论：PASS/REJECT", example = "PASS")
    private String auditStatus;

    @Schema(description = "审核意见")
    private String auditOpinion;
}