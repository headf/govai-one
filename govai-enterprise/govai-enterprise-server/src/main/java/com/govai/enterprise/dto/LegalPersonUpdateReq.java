package com.govai.enterprise.dto;

import lombok.Data;

/**
 * 修改法定代表人信息请求
 */
@Data
public class LegalPersonUpdateReq {

    /** 法定代表人姓名 */
    private String legalPerson;

    /** 法定代表人身份证号 */
    private String legalPersonIdCard;
}
