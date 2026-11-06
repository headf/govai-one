package com.govai.supervision.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 权限实体
 */
@Data
@TableName("sys_permission")
public class SysPermission implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 权限名称 */
    private String permissionName;

    /** 权限编码（唯一） */
    private String permissionCode;

    /** 类型：MENU/BUTTON/API */
    private String type;

    /** 父权限ID，0 为根 */
    private Long parentId;

    /** 排序 */
    private Integer sortOrder;

    /** 前端路由路径 */
    private String path;

    private LocalDateTime createTime;

    /** 子权限（用于构建树，非表字段） */
    @TableField(exist = false)
    private List<SysPermission> children;
}