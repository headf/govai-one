package com.govai.enterprise.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 企业电子证照表 enterprise_license
 */
@Data
@TableName("enterprise_license")
public class EnterpriseLicense {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 企业ID */
    private Long enterpriseId;

    /** 证照类型 营业执照/资质证书/许可证 */
    private String licenseType;

    /** 证照类别 ENTERPRISE企业/PERSONAL个人 */
    private String category;

    /** 证照编号 */
    private String licenseNo;

    /** 证照名称 */
    private String licenseName;

    /** 发证日期 */
    private LocalDate issueDate;

    /** 有效期至 */
    private LocalDate expiryDate;

    /** 文件URL */
    private String fileUrl;

    /** 1有效 0失效 */
    private Integer status;

    /** 作废原因 RENEW_EXTEND延续换发/RENEW_CHANGE变更换发/CANCEL_SELF自行注销/CANCEL_ENT企业注销 */
    private String voidReason;

    private LocalDateTime createTime;
}
