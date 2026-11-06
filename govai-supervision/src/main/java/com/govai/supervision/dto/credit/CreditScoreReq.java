package com.govai.supervision.dto.credit;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 信用评分更新请求（企业/个人通用）
 */
@Data
@Schema(description = "信用评分更新请求")
public class CreditScoreReq implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "目标ID不能为空")
    @Schema(description = "企业ID 或 用户ID")
    private Long targetId;

    @NotNull(message = "评分不能为空")
    @DecimalMin(value = "0", message = "评分不能低于0")
    @DecimalMax(value = "100", message = "评分不能高于100")
    @Schema(description = "信用评分 0-100")
    private BigDecimal creditScore;

    @Schema(description = "各维度得分(JSON)")
    private String dimensionScores;
}