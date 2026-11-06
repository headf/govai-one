package com.govai.citizen.dto;

import lombok.Data;

@Data
public class ApplyResultVO {

    private Long recordId;
    private String recordCode;
    private Long approvalInstanceId;
    private String status;
}
