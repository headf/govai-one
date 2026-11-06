package com.govai.citizen.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("progress_notification")
public class ProgressNotification {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long recordId;
    private String nodeName;
    private String content;
    private String notifyType;
    private Integer sendStatus;
    private LocalDateTime sendTime;
    private LocalDateTime createTime;
}
