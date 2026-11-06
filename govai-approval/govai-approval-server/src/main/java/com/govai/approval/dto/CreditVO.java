package com.govai.approval.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 企业信用视图(审批端跨端查监管端信用展示用)
 */
@Data
public class CreditVO {

    /** 信用评分(0-100) */
    private BigDecimal creditScore;

    /** 信用等级 A/B/C/D */
    private String creditLevel;
}
