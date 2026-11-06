package com.govai.enterprise.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 法人事项信息表 ent_matter_info
 */
@Data
@TableName("ent_matter_info")
public class EntMatterInfo {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 事项编码 */
    private String matterCode;

    /** 事项名称 */
    private String matterName;

    /** 分类ID */
    private Long categoryId;

    /** 实施部门 */
    private String department;

    /** 事项描述 */
    private String description;

    /** 办理指南(富文本) */
    private String guideContent;

    /** 申报条件(JSON数组) */
    private String conditions;

    /** 所需材料(JSON数组) */
    private String materials;

    /** 办理流程(JSON数组) */
    private String processSteps;

    /** 法定办结天数 */
    private Integer deadlineDays;

    /** 申报表单字段配置(JSON,动态表单渲染) */
    private String formConfig;

    /** 1上线 0下线 */
    private Integer status;

    private LocalDateTime createTime;
}
