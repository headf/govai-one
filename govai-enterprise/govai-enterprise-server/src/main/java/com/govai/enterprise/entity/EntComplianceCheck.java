package com.govai.enterprise.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 合规预检记录表 ent_compliance_check
 */
@Data
@TableName("ent_compliance_check")
public class EntComplianceCheck {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 申报记录ID */
    private Long recordId;

    /** 检查项 */
    private String checkItem;

    /** 1通过 0不通过 */
    private Integer checkResult;

    /** 检查详情 */
    private String checkDetail;

    private LocalDateTime checkTime;
}
