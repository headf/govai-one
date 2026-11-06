package com.govai.enterprise.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 企业申报记录表 ent_application_record
 */
@Data
@TableName("ent_application_record")
public class EntApplicationRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 申报编号 */
    private String recordCode;

    /** 企业ID */
    private Long enterpriseId;

    /** 事项ID */
    private Long matterId;

    /** 经办人(用户)ID */
    private Long operatorId;

    /** 表单数据(JSON) */
    private String formData;

    /** 状态 PENDING/APPROVING/PASSED/REJECTED/REPAIR */
    private String status;

    /** 关联审批实例ID(跨服务,由审批端返回) */
    private Long approvalInstanceId;

    /** 提交时间 */
    private LocalDateTime submitTime;

    /** 办结时间 */
    private LocalDateTime finishTime;

    /** 审批端退回/补正意见(REPAIR/REJECTED 时写入) */
    private String remark;

    private LocalDateTime createTime;
}
