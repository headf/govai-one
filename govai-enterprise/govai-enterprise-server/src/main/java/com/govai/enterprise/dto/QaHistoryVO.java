package com.govai.enterprise.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 智能问答历史记录 VO(也用作会话消息项)
 */
@Data
@Schema(description = "智能问答历史记录")
public class QaHistoryVO {

    @Schema(description = "记录ID")
    private Long id;

    @Schema(description = "问题")
    private String question;

    @Schema(description = "回答")
    private String answer;

    @Schema(description = "引用来源")
    private List<QaAskResp.Source> sources;

    @Schema(description = "提问时间")
    private LocalDateTime createTime;
}
