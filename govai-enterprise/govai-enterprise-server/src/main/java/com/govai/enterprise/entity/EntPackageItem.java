package com.govai.enterprise.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 套餐事项关联表 ent_package_item
 */
@Data
@TableName("ent_package_item")
public class EntPackageItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 套餐ID */
    private Long packageId;

    /** 事项ID */
    private Long matterId;

    /** 排序 */
    private Integer sortOrder;
}
