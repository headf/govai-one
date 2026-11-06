package com.govai.supervision.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 企业信用评价实体
 */
@Data
@TableName("credit_enterprise")
public class CreditEnterprise implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 企业ID（跨服务关联企业端） */
    private Long enterpriseId;

    /** 信用评分（0-100 默认100） */
    private BigDecimal creditScore;

    /** 信用等级 A/B/C/D */
    private String creditLevel;

    /** 各维度得分(JSON) */
    private String dimensionScores;

    /** 企业名称（非表字段，跨端回填） */
    @TableField(exist = false)
    private String enterpriseName;

    /** 统一社会信用代码(真列,审批端按此关联企业信用) */
    private String creditCode;

    private LocalDateTime updateTime;
}