package com.govai.citizen.dto;

import lombok.Data;

@Data
public class MatterQuery {

    private String keyword;
    private Long categoryId;
    private Integer page = 1;
    private Integer size = 10;
}
