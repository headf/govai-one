package com.govai.supervision.dto.risk;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 风险预警规则编辑请求
 */
@Data
@Schema(description = "风险预警规则编辑")
public class RiskRuleDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "规则ID（新增时为空）")
    private Long id;

    @NotBlank(message = "规则名称不能为空")
    @Schema(description = "规则名称")
    private String ruleName;

    /** TIMEOUT/ANOMALY/THRESHOLD */
    @NotBlank(message = "规则类型不能为空")
    @Schema(description = "规则类型：TIMEOUT/ANOMALY/THRESHOLD", example = "TIMEOUT")
    private String ruleType;

    @Schema(description = "阈值（如超时小时数/申请量）")
    private String thresholdValue;

    /** LOW/MEDIUM/HIGH */
    @NotBlank(message = "预警级别不能为空")
    @Schema(description = "预警级别：LOW/MEDIUM/HIGH")
    private String alertLevel;

    @Schema(description = "通知方式：SMS/EMAIL/SITE")
    private String notifyType;

    @Schema(description = "状态 1启用 0停用")
    private Integer status;
}