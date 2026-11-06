package com.govai.approval.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 规则集表
 */
@Data
@TableName("approval_rule_set")
public class ApprovalRuleSet implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String ruleSetName;
    private String matterType;
    private Integer status;
    /** AND/OR 顶层逻辑 */
    private String logicType;
    private String description;
    private LocalDateTime createTime;
}
