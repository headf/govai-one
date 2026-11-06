package com.govai.enterprise.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 一件事套餐表 ent_package
 */
@Data
@TableName("ent_package")
public class EntPackage {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 套餐编码 */
    private String packageCode;

    /** 套餐名称 */
    private String packageName;

    /** 套餐说明 */
    private String description;

    /** 1上线 0下线 */
    private Integer status;

    private LocalDateTime createTime;
}
