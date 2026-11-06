package com.govai.citizen.dto;

import lombok.Data;

@Data
public class MatterListVO {

    private Long id;
    private String matterCode;
    private String matterName;
    private String department;
    private String description;
    private Long categoryId;
    private String categoryName;
    private Integer deadlineDays;
}
