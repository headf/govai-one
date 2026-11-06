package com.govai.supervision.service;

import com.govai.supervision.common.PageResult;
import com.govai.supervision.dto.credit.CreditScoreReq;
import com.govai.supervision.entity.CreditPersonal;

import java.math.BigDecimal;

/**
 * 个人信用服务
 */
public interface CreditPersonalService {

    /** 分页查询个人信用 */
    PageResult<CreditPersonal> pagePersonal(long page, long size, Long userId, String level);

    /** 查询单用户信用 */
    CreditPersonal getByUserId(Long userId);

    /** 评分（新增/更新） */
    CreditPersonal saveOrUpdateScore(CreditScoreReq req);

    /** 加分（信用修复通过等场景），上限 100 */
    BigDecimal addScoreBonus(Long userId, BigDecimal bonus);

    /** 删除个人信用 */
    void deleteByUserId(Long userId);
}