package com.govai.enterprise.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 补贴发放记录表 subsidy_payment(扶持补贴/减免事项办结后到账)
 */
@Data
@TableName("subsidy_payment")
public class SubsidyPayment {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 企业ID */
    private Long enterpriseId;

    /** 事项ID(补贴/减免事项) */
    private Long matterId;

    /** 申报记录ID */
    private Long recordId;

    /** 补贴类型(稳岗返还/增值税减免等) */
    private String subsidyType;

    /** 发放金额(元) */
    private BigDecimal amount;

    /** 到账时间 */
    private LocalDateTime payTime;

    /** PAID已发放 */
    private String status;

    /** 收款银行 */
    private String bankName;

    /** 收款账号 */
    private String bankAccount;

    private LocalDateTime createTime;
}
