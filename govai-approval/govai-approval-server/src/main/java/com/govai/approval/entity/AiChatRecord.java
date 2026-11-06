package com.govai.approval.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 对话记录表
 */
@Data
@TableName("ai_chat_record")
public class AiChatRecord implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long instanceId;
    private Long templateId;
    private String userMessage;
    private String aiResponse;
    private Integer tokenCount;
    private Long responseTimeMs;
    private LocalDateTime createTime;
}
