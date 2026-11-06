package com.govai.enterprise.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 智能问答会话 VO(会话列表项)
 */
@Data
@Schema(description = "智能问答会话")
public class QaSessionVO {

    @Schema(description = "会话ID")
    private Long id;

    @Schema(description = "会话标题")
    private String title;

    @Schema(description = "消息数")
    private Integer messageCount;

    @Schema(description = "最后活跃时间")
    private LocalDateTime lastTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
