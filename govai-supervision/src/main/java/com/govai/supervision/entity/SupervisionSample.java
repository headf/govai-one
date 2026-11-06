package com.govai.supervision.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 抽样检查记录实体
 */
@Data
@TableName("supervision_sample")
public class SupervisionSample implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 抽查单号(CJ+日期+随机),供前端展示替代裸自增id */
    private String sampleNo;

    /** 关联审批实例ID */
    private Long instanceId;

    /** 抽样类型：RANDOM/TARGETED */
    private String sampleType;

    /** 检查结果：PASS/FAIL */
    private String checkResult;

    /** 检查详情 */
    private String checkDetail;

    /** 检查人ID */
    private Long checkerId;

    private LocalDateTime checkTime;
}