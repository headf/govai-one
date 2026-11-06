package com.govai.citizen.dto;

import lombok.Data;

import java.util.List;

@Data
public class MatterSaveReq {

    private Long id;
    private String matterCode;
    private String matterName;
    private Long categoryId;
    private String department;
    private String description;
    private String guideContent;
    private List<String> conditions;
    private List<String> materials;
    private List<String> processSteps;
    private Integer deadlineDays;
    private Integer status;
    private Integer sortOrder;
}
