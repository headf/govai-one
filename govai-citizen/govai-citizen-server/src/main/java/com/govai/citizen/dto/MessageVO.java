package com.govai.citizen.dto;

import com.govai.citizen.entity.SiteMessage;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MessageVO {

    private Long id;
    private String messageType;
    private String title;
    private String content;
    private Long recordId;
    private Integer isRead;
    private LocalDateTime createTime;

    public static MessageVO from(SiteMessage m) {
        MessageVO vo = new MessageVO();
        vo.setId(m.getId());
        vo.setMessageType(m.getMessageType());
        vo.setTitle(m.getTitle());
        vo.setContent(m.getContent());
        vo.setRecordId(m.getRecordId());
        vo.setIsRead(m.getIsRead());
        vo.setCreateTime(m.getCreateTime());
        return vo;
    }
}
