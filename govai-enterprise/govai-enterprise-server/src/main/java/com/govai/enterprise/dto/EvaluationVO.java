package com.govai.enterprise.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 好差评 VO(附申报编号/事项名)
 */
@Data
@Schema(description = "好差评记录")
public class EvaluationVO {

    @Schema(description = "评价ID")
    private Long id;

    @Schema(description = "申报记录ID")
    private Long recordId;

    @Schema(description = "申报编号")
    private String recordCode;

    @Schema(description = "事项名称")
    private String matterName;

    @Schema(description = "星级 1-5")
    private Integer starRating;

    @Schema(description = "文字评价")
    private String content;

    @Schema(description = "快捷标签")
    private List<String> tags;

    @Schema(description = "评价时间")
    private LocalDateTime createTime;
}
