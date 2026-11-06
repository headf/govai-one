package com.govai.citizen.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class EvaluationSubmitReq {

    @NotNull(message = "申报记录不能为空")
    private Long recordId;

    @NotNull(message = "请选择星级")
    @Min(value = 1, message = "星级最小1")
    @Max(value = 5, message = "星级最大5")
    private Integer starRating;

    private String content;

    private List<String> tags;
}
