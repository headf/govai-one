package com.govai.enterprise.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 事项大厅查询条件
 */
@Data
@Schema(description = "事项列表查询条件")
public class MatterQuery {

    @Schema(description = "关键词(按事项名称/编码模糊匹配)")
    private String keyword;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "当前页,默认1")
    private Integer page = 1;

    @Schema(description = "每页条数,默认10")
    private Integer size = 10;
}
