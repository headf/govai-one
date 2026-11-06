package com.govai.enterprise.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 法人事项分类表 ent_matter_category
 */
@Data
@TableName("ent_matter_category")
public class EntMatterCategory {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 分类名称 */
    private String categoryName;

    /** 父分类ID */
    private Long parentId;

    /** 排序 */
    private Integer sortOrder;

    private LocalDateTime createTime;
}
