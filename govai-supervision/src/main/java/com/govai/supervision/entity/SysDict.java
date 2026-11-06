package com.govai.supervision.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 数据字典实体
 */
@Data
@TableName("sys_dict")
public class SysDict implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 字典类型（如 credit_level / alert_level / alert_status） */
    private String dictType;

    /** 显示名（如 A(优秀)） */
    private String dictLabel;

    /** 字典值（如 A） */
    private String dictValue;

    private Integer sortOrder;

    /** 1启用 0禁用 */
    private Integer status;

    private LocalDateTime createTime;
}
