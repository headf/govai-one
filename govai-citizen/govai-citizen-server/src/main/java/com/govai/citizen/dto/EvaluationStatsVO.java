package com.govai.citizen.dto;

import lombok.Data;

@Data
public class EvaluationStatsVO {

    private long total;
    private double avgStar;
    private long fiveStar;
    private long fourStar;
    private long threeStar;
    private long twoStar;
    private long oneStar;
    private double positiveRate;
}
