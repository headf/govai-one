package com.govai.enterprise.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 企业站内消息表 ent_message(办结/证照/政策等提醒聚合, 未读角标)
 */
@Data
@TableName("ent_message")
public class EntMessage {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long enterpriseId;
    private String msgType;
    private String title;
    private String content;
    private Integer isRead;
    private LocalDateTime createTime;
}
