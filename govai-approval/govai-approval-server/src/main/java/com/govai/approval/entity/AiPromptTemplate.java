package com.govai.approval.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI Prompt 模板表
 */
@Data
@TableName("ai_prompt_template")
public class AiPromptTemplate implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String templateName;
    /** OPINION/POLICY_SUMMARY/RISK */
    private String sceneType;
    private String systemPrompt;
    /** 用户提示词模板({变量}) */
    private String userPromptTemplate;
    /** 变量列表(JSON) */
    private String variables;
    private Integer status;
    private LocalDateTime createTime;
}
