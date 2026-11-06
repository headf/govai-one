package com.govai.enterprise.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 好差评提交请求
 */
@Data
@Schema(description = "好差评提交请求")
public class EvaluationSubmitReq {

    @Schema(description = "申报记录ID")
    @NotNull(message = "申报记录不能为空")
    private Long recordId;

    @Schema(description = "星级 1-5")
    @NotNull(message = "星级不能为空")
    @Min(value = 1, message = "星级最小为1")
    @Max(value = 5, message = "星级最大为5")
    private Integer starRating;

    @Schema(description = "文字评价(可空)")
    private String content;

    @Schema(description = "快捷标签")
    private List<String> tags;
}
