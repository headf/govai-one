package com.govai.citizen.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UploadVO {

    private String fileUrl;
    private String fileName;
    private String fileType;
    private Long size;
    private LocalDateTime uploadTime;
}
