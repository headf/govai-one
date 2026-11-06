package com.govai.enterprise.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 合规预检结果
 */
@Data
@Schema(description = "合规预检报告")
public class PreCheckResp {

    @Schema(description = "是否全部通过")
    private Boolean passed;

    @Schema(description = "通过项数")
    private Integer passCount;

    @Schema(description = "不通过项数")
    private Integer failCount;

    @Schema(description = "检查明细")
    private List<Item> items;

    @Data
    public static class Item {
        @Schema(description = "检查项")
        private String checkItem;
        @Schema(description = "是否通过")
        private Boolean result;
        @Schema(description = "说明")
        private String detail;
    }
}
