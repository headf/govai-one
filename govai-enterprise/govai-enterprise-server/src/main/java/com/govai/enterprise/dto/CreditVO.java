package com.govai.enterprise.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 企业信用视图(企业端跨端查监管端信用)
 * 只取企业端需要的字段,不跨端引监管端实体。
 */
@Data
public class CreditVO {

    /** 信用评分(0-100) */
    private BigDecimal creditScore;

    /** 信用等级 A/B/C/D */
    private String creditLevel;
}
