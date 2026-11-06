package com.govai.citizen.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("site_message")
public class SiteMessage {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String messageType;
    private String title;
    private String content;
    private Long recordId;
    private Integer isRead;
    private LocalDateTime readTime;
    private LocalDateTime createTime;
}
