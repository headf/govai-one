package com.govai.supervision.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统操作日志实体
 */
@Data
@TableName("sys_log")
public class SysLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String username;

    /** 操作描述（如 新增用户） */
    private String operation;

    /** 请求方法路径（如 POST /api/v1/users） */
    private String method;

    /** 请求参数(JSON) */
    private String params;

    private String ip;

    /** 耗时 ms */
    private Long costTime;

    private LocalDateTime createTime;
}
