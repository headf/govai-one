package com.govai.enterprise.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 企业摘要 VO(跨服务只读返回,不含敏感法人信息)
 */
@Data
public class EnterpriseBriefVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long enterpriseId;

    private String enterpriseName;

    private String creditCode;
}
