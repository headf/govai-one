package com.govai.enterprise.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 套餐一表申报提交结果(拆分后的多个申报)
 */
@Data
@Schema(description = "套餐申报提交结果")
public class PackageSubmitResp {

    @Schema(description = "拆分出的申报记录")
    private List<Item> records;

    @Data
    @Schema(description = "拆分出的单条申报")
    public static class Item {
        @Schema(description = "申报记录ID")
        private Long recordId;
        @Schema(description = "申报编号")
        private String recordCode;
        @Schema(description = "事项ID")
        private Long matterId;
        @Schema(description = "事项名称")
        private String matterName;
        @Schema(description = "状态")
        private String status;
    }
}
