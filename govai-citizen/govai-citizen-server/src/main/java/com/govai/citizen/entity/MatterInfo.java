package com.govai.citizen.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("matter_info")
public class MatterInfo {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String matterCode;
    private String matterName;
    private Long categoryId;
    private String department;
    private String description;
    private String guideContent;
    private String conditions;
    private String materials;
    private String processSteps;
    private Integer deadlineDays;
    private Integer status;
    private Integer sortOrder;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
