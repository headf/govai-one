package com.govai.enterprise.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 企业智能问答记录表 ent_qa_record
 */
@Data
@TableName("ent_qa_record")
public class EntQaRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 企业ID */
    private Long enterpriseId;

    /** 提问人(企业用户)ID */
    private Long userId;

    /** 所属会话ID */
    private Long sessionId;

    /** 问题 */
    private String question;

    /** 回答 */
    private String answer;

    /** 引用来源(JSON数组) */
    private String sources;

    private LocalDateTime createTime;
}
