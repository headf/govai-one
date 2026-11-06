package com.govai.citizen.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EvaluationVO {

    private Long id;
    private Long recordId;
    private String matterName;
    private Integer starRating;
    private String content;
    private java.util.List<String> tags;
    private LocalDateTime createTime;
}
