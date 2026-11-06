package com.govai.supervision.service;

import com.govai.supervision.common.PageResult;
import com.govai.supervision.dto.credit.CreditScoreReq;
import com.govai.supervision.entity.CreditEnterprise;

import java.math.BigDecimal;

/**
 * 企业信用服务
 */
public interface CreditEnterpriseService {

    /** 分页查询企业信用 */
    PageResult<CreditEnterprise> pageEnterprise(long page, long size, Long enterpriseId, String level);

    /** 查询单企业信用 */
    CreditEnterprise getByEnterpriseId(Long enterpriseId);

    /** 按统一社会信用代码查询(审批端等只有信用代码的场景关联企业信用) */
    CreditEnterprise getByCreditCode(String creditCode);

    /** 新增/更新评分（按评分自动算等级） */
    CreditEnterprise saveOrUpdateScore(CreditScoreReq req);

    /** 手动调整评分 */
    BigDecimal adjustScore(Long enterpriseId, BigDecimal newScore);

    /** 加分（信用修复通过等场景），上限 100 */
    BigDecimal addScoreBonus(Long enterpriseId, BigDecimal bonus);

    /** 删除 */
    void deleteByEnterpriseId(Long enterpriseId);
}