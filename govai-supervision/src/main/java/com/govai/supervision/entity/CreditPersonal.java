package com.govai.supervision.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 个人信用评价实体
 */
@Data
@TableName("credit_personal")
public class CreditPersonal implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 个人信用单号(CX+日期+随机),供前端展示替代裸自增id */
    private String creditNo;

    /** 用户ID（跨服务关联群众/企业端用户） */
    private Long userId;

    /** 用户姓名（非表字段，分页时回填 sys_user.real_name，供前端展示） */
    @TableField(exist = false)
    private String userName;

    /** 信用评分 */
    private BigDecimal creditScore;

    /** 信用等级 */
    private String creditLevel;

    private LocalDateTime updateTime;
}