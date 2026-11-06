package com.govai.citizen.dto;

import com.govai.citizen.entity.PolicyRecommend;
import lombok.Data;

@Data
public class PolicyVO {

    private Long id;
    private String policyTitle;
    private String matchReason;
    private Integer isRead;
    private String createTime;

    public static PolicyVO from(PolicyRecommend p) {
        PolicyVO vo = new PolicyVO();
        vo.setId(p.getId());
        vo.setPolicyTitle(p.getPolicyTitle());
        vo.setMatchReason(p.getMatchReason());
        vo.setIsRead(p.getIsRead());
        vo.setCreateTime(p.getCreateTime() == null ? null : p.getCreateTime().toString());
        return vo;
    }
}
