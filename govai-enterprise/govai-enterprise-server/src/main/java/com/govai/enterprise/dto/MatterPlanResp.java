package com.govai.enterprise.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "智能办事规划结果")
public class MatterPlanResp {

    @Schema(description = "规划标题(如:开办食品销售公司办理清单)")
    private String title;

    @Schema(description = "整体说明与办理顺序建议")
    private String summary;

    @Schema(description = "推荐办理事项列表(按办理先后顺序)")
    private List<PlanItem> items;

    @Schema(description = "额外提示/常见问题")
    private List<String> tips;

    @Schema(description = "规划历史ID(用于记忆/恢复)")
    private Long historyId;

    @Schema(description = "是否已完成确认")
    private Boolean completed;

    @Data
    @Schema(description = "规划中的单个事项")
    public static class PlanItem {

        @Schema(description = "事项ID(对应 ent_matter_info.id,前端可直接跳转)")
        private Long matterId;

        @Schema(description = "事项名称")
        private String matterName;

        @Schema(description = "实施部门")
        private String department;

        @Schema(description = "建议办理顺序(从1开始)")
        private Integer order;

        @Schema(description = "推荐理由/该事项在整体规划中的作用")
        private String reason;

        @Schema(description = "办理指南摘要")
        private String guide;

        @Schema(description = "所需材料")
        private List<String> materials;

        @Schema(description = "办理流程步骤")
        private List<String> process;

        @Schema(description = "办结时限(工作日)")
        private Integer deadlineDays;

        @Schema(description = "是否为前置必需(营业执照等)")
        private Boolean prerequisite;

        @Schema(description = "状态: RECOMMENDED-推荐 | OPTIONAL-可选 | SKIPPED-已具备可跳过")
        private String status;
    }
}
