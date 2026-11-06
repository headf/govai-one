package com.govai.enterprise.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 企业法人+成员(跨服务只读,供监管端展示企业人员构成)
 */
@Data
public class EnterpriseMemberVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long enterpriseId;

    /** 工商登记法人姓名 */
    private String legalPerson;

    /** 工商登记法人身份证号 */
    private String legalPersonIdCard;

    /** 企业登录账号成员(含法人账号 ADMIN + 经办人 OPERATOR) */
    private List<EnterpriseMemberItemVO> members;
}
