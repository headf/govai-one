package com.govai.supervision.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 差评整改工单实体
 *
 * 来源:群众端/企业端提交好差评时,星级<=2 的差评由两端通过 Feign 上报,在本表落一条待处置工单,
 * 监管端在"差评整改"页面处置,形成"差评→整改→反馈"闭环(国办对好差评"差评必整改"的要求)。
 */
@Data
@TableName("review_order")
public class ReviewOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工单编号(XP+日期+随机),供前端展示替代裸自增 id */
    private String reviewNo;

    /** 来源: CITIZEN 群众端 / ENTERPRISE 企业端 */
    private String source;

    /** 评价人 */
    private String applicantName;

    /** 申报事项名称 */
    private String matterName;

    /** 申报编号 */
    private String recordCode;

    /** 星级 1-5 */
    private Integer starRating;

    /** 评价内容 */
    private String content;

    /** 评价标签(逗号分隔) */
    private String tags;

    /** 处置状态: PENDING 待处置 / HANDLED 已处置 */
    private String handleStatus;

    /** 整改处置意见 */
    private String handleOpinion;

    /** 处置人 */
    private String handlerName;

    /** 上报时间 */
    private LocalDateTime createTime;

    /** 处置时间 */
    private LocalDateTime handleTime;
}
