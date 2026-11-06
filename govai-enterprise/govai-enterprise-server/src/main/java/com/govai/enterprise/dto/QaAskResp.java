package com.govai.enterprise.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 智能问答响应
 */
@Data
@Schema(description = "智能问答响应")
public class QaAskResp {

    @Schema(description = "回答内容")
    private String answer;

    @Schema(description = "引用来源")
    private List<Source> sources;

    @Schema(description = "关联事项")
    private List<RelatedMatter> relatedMatters;

    @Schema(description = "推荐追问")
    private List<String> suggestedQuestions;

    @Data
    @Schema(description = "引用来源")
    public static class Source {
        @Schema(description = "类型 POLICY政策/MATTER事项")
        private String type;
        @Schema(description = "标题")
        private String title;
        @Schema(description = "说明/摘要")
        private String snippet;
    }

    @Data
    @Schema(description = "关联事项")
    public static class RelatedMatter {
        @Schema(description = "事项ID")
        private Long matterId;
        @Schema(description = "事项名称")
        private String matterName;
        @Schema(description = "事项编码")
        private String matterCode;
    }
}
