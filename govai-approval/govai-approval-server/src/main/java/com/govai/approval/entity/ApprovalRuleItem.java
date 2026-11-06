package com.govai.approval.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 规则项表
 */
@Data
@TableName("approval_rule_item")
public class ApprovalRuleItem implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long ruleSetId;
    /** NUMBER/TEXT/DATE/LICENSE */
    private String ruleType;
    private String fieldName;
    /** GT/LT/EQ/CONTAINS/BETWEEN */
    private String operator;
    private String compareValue;
    /** 与上一条规则的逻辑 AND/OR */
    private String logicOperator;
    private Integer sortOrder;
    private String errorMessage;
}
