package com.govai.citizen.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RecordListVO {

    private Long id;
    private String recordCode;
    private Long matterId;
    private String matterName;
    private String status;
    private String statusText;
    private LocalDateTime submitTime;
    private LocalDateTime finishTime;
    private Long approvalInstanceId;
}
