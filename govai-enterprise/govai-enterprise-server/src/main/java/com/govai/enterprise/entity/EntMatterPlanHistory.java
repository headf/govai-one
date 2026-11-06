package com.govai.enterprise.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 智能办事规划历史(记忆功能)
 */
@Data
@TableName("ent_matter_plan_history")
public class EntMatterPlanHistory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long enterpriseId;
    private Long userId;
    private String description;
    private Integer hasLicense;
    private String title;
    private String summary;
    private String planJson;
    private LocalDateTime createTime;
}
