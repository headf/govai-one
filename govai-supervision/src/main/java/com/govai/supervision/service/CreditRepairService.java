package com.govai.supervision.service;

import com.govai.supervision.common.PageResult;
import com.govai.supervision.dto.credit.CreditRepairAuditReq;
import com.govai.supervision.dto.credit.CreditRepairReq;
import com.govai.supervision.entity.CreditRepair;

/**
 * 信用修复服务
 */
public interface CreditRepairService {

    /** 提交修复申请 */
    CreditRepair applyCreditRepair(CreditRepairReq req);

    /** 分页查询申请（可按状态） */
    PageResult<CreditRepair> pageRepairs(long page, long size, String status, String targetType);

    /** 审核：PASS/REJECT（仅 PENDING 可审） */
    CreditRepair audit(CreditRepairAuditReq req);
}