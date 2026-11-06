package com.govai.enterprise.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 企业信息表 enterprise_info
 */
@Data
@TableName("enterprise_info")
public class EnterpriseInfo {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 统一社会信用代码 */
    private String creditCode;

    /** 企业名称 */
    private String enterpriseName;

    /** 法定代表人 */
    private String legalPerson;

    /** 法定代表人身份证号 */
    private String legalPersonIdCard;

    /** 注册地址 */
    private String registerAddress;

    /** 联系电话 */
    private String contactPhone;

    /** 企业类型 */
    private String enterpriseType;

    /** 经营范围 */
    private String businessScope;

    /** 注册资本(万元) */
    private BigDecimal registerCapital;

    /** 经营期限至(长期为空) */
    private LocalDate businessTermUntil;

    /** 成立日期 */
    private LocalDate establishDate;

    /** 1正常 0注销 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
