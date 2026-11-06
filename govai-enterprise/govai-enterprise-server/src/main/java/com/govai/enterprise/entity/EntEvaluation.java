package com.govai.enterprise.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 企业好差评表 ent_evaluation
 */
@Data
@TableName("ent_evaluation")
public class EntEvaluation {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 申报记录ID */
    private Long recordId;

    /** 企业ID */
    private Long enterpriseId;

    /** 评价人(企业用户)ID */
    private Long userId;

    /** 星级 1-5 */
    private Integer starRating;

    /** 文字评价 */
    private String content;

    /** 标签(JSON数组) */
    private String tags;

    private LocalDateTime createTime;
}
