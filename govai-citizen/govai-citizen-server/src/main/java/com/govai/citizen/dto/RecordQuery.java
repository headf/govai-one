package com.govai.citizen.dto;

import lombok.Data;

@Data
public class RecordQuery {

    private String status;
    private Integer page = 1;
    private Integer size = 10;
}
