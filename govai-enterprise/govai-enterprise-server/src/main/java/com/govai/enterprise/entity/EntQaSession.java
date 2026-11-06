package com.govai.enterprise.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 企业智能问答会话表 ent_qa_session
 */
@Data
@TableName("ent_qa_session")
public class EntQaSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 企业ID */
    private Long enterpriseId;

    /** 提问人(企业用户)ID */
    private Long userId;

    /** 会话标题(首条问题前20字) */
    private String title;

    /** 消息数 */
    private Integer messageCount;

    /** 最后活跃时间 */
    private LocalDateTime lastTime;

    private LocalDateTime createTime;
}
