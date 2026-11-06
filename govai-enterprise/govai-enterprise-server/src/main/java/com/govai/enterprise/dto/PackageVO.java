package com.govai.enterprise.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 一件事套餐 VO(含事项明细 + 合并后的表单/材料)
 */
@Data
@Schema(description = "一件事套餐")
public class PackageVO {

    @Schema(description = "套餐ID")
    private Long id;

    @Schema(description = "套餐编码")
    private String packageCode;

    @Schema(description = "套餐名称")
    private String packageName;

    @Schema(description = "套餐说明")
    private String description;

    @Schema(description = "套餐包含的事项")
    private List<PackageItemVO> items;

    @Schema(description = "合并后的申报表单(一表申请,共用字段去重)")
    private List<MatterVO.FormFieldConfig> mergedForm;

    @Schema(description = "合并去重后的材料清单")
    private List<String> mergedMaterials;

    @Schema(description = "合并后的办理流程")
    private List<String> processSteps;

    @Schema(description = "合计承诺时限(工作日)")
    private Integer totalDeadlineDays;

    /**
     * 套餐事项明细
     */
    @Data
    @Schema(description = "套餐事项明细")
    public static class PackageItemVO {
        @Schema(description = "事项ID")
        private Long matterId;
        @Schema(description = "事项编码")
        private String matterCode;
        @Schema(description = "事项名称")
        private String matterName;
        @Schema(description = "实施部门")
        private String department;
        @Schema(description = "承诺时限")
        private Integer deadlineDays;
        @Schema(description = "该事项表单配置")
        private List<MatterVO.FormFieldConfig> formConfig;
        @Schema(description = "该事项材料清单")
        private List<String> materials;
    }
}
