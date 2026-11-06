package com.govai.enterprise.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 企业政策匹配表 ent_policy_match(免申即享确认闭环)
 */
@Data
@TableName("ent_policy_match")
public class EntPolicyMatch {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 企业ID */
    private Long enterpriseId;

    /** 政策标题 */
    private String policyTitle;

    /** 政策内容 */
    private String policyContent;

    /** 匹配度(0-100) */
    private BigDecimal matchScore;

    /** 0待确认 1已确认享受 */
    private Integer isApplied;

    /** 确认时间 */
    private LocalDateTime applyTime;

    private LocalDateTime createTime;
}
