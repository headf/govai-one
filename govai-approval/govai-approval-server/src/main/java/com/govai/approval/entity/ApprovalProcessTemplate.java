package com.govai.approval.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 流程模板表
 */
@Data
@TableName("approval_process_template")
public class ApprovalProcessTemplate implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String templateCode;
    private String templateName;
    private String version;
    private String matterType;
    /** 1启用 0停用 */
    private Integer status;
    /** 环节配置(JSON)，冗余存储便于前端展示 */
    private String nodeConfig;
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
