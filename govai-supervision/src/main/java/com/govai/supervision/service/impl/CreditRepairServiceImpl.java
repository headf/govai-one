package com.govai.supervision.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.govai.supervision.common.BizNoGenerator;
import com.govai.supervision.common.BusinessException;
import com.govai.supervision.common.PageResult;
import com.govai.supervision.dto.credit.CreditRepairAuditReq;
import com.govai.supervision.dto.credit.CreditRepairReq;
import com.govai.supervision.entity.CreditRepair;
import com.govai.supervision.mapper.CreditRepairMapper;
import com.govai.supervision.service.CreditEnterpriseService;
import com.govai.supervision.service.CreditPersonalService;
import com.govai.supervision.service.CreditRepairService;
import com.govai.supervision.util.EnterpriseProfileClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 信用修复服务实现
 */
@Service
@RequiredArgsConstructor
public class CreditRepairServiceImpl implements CreditRepairService {

    private final CreditRepairMapper creditRepairMapper;
    private final CreditEnterpriseService creditEnterpriseService;
    private final CreditPersonalService creditPersonalService;
    private final EnterpriseProfileClient enterpriseProfileClient;

    /** 修复通过后的信用分加分 */
    private static final BigDecimal PASS_BONUS = new BigDecimal("5");

    @Override
    public CreditRepair applyCreditRepair(CreditRepairReq req) {
        // 避免对同一目标重复提交未审核申请
        Long pending = creditRepairMapper.selectCount(new LambdaQueryWrapper<CreditRepair>()
                .eq(CreditRepair::getTargetType, req.getTargetType())
                .eq(CreditRepair::getTargetId, req.getTargetId())
                .eq(CreditRepair::getAuditStatus, "PENDING"));
        if (pending != null && pending > 0) {
            throw new BusinessException(400, "该主体存在待审核的修复申请，请勿重复提交");
        }
        CreditRepair repair = new CreditRepair();
        repair.setRepairNo(BizNoGenerator.gen("XF"));
        repair.setTargetType(req.getTargetType());
        repair.setTargetId(req.getTargetId());
        repair.setRepairReason(req.getRepairReason());
        repair.setApplyTime(LocalDateTime.now());
        repair.setAuditStatus("PENDING");
        creditRepairMapper.insert(repair);
        return repair;
    }

    @Override
    public PageResult<CreditRepair> pageRepairs(long page, long size, String status, String targetType) {
        LambdaQueryWrapper<CreditRepair> wrapper = new LambdaQueryWrapper<CreditRepair>()
                .eq(status != null && !status.isBlank(), CreditRepair::getAuditStatus, status)
                .eq(targetType != null && !targetType.isBlank(), CreditRepair::getTargetType, targetType)
                .orderByDesc(CreditRepair::getApplyTime);
        PageResult<CreditRepair> result = PageResult.of(creditRepairMapper.selectPage(new Page<>(page, size), wrapper));
        fillTargetNames(result.getList());
        return result;
    }

    /** 回填主体名称(企业主体跨端查企业名),供前端展示替代裸主体ID */
    private void fillTargetNames(java.util.List<CreditRepair> list) {
        java.util.List<Long> entIds = list.stream()
                .filter(r -> "ENTERPRISE".equals(r.getTargetType()) && r.getTargetId() != null)
                .map(CreditRepair::getTargetId)
                .distinct()
                .collect(java.util.stream.Collectors.toList());
        if (entIds.isEmpty()) {
            return;
        }
        java.util.Map<Long, EnterpriseProfileClient.Brief> briefs = enterpriseProfileClient.fetchBriefs(entIds);
        if (briefs.isEmpty()) {
            return;
        }
        for (CreditRepair r : list) {
            if ("ENTERPRISE".equals(r.getTargetType())) {
                EnterpriseProfileClient.Brief b = briefs.get(r.getTargetId());
                if (b != null) {
                    r.setTargetName(b.getEnterpriseName());
                }
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreditRepair audit(CreditRepairAuditReq req) {
        CreditRepair repair = creditRepairMapper.selectById(req.getId());
        if (repair == null) {
            throw new BusinessException(404, "申请不存在");
        }
        if (!"PENDING".equals(repair.getAuditStatus())) {
            throw new BusinessException(400, "仅待审核状态可审核");
        }
        String result = req.getAuditStatus();
        if (!"PASS".equals(result) && !"REJECT".equals(result)) {
            throw new BusinessException(400, "审核结论必须为 PASS 或 REJECT");
        }
        // 驳回必须说明理由，便于主体整改后重新提交
        if ("REJECT".equals(result) && !StringUtils.hasText(req.getAuditOpinion())) {
            throw new BusinessException(400, "驳回时必须填写驳回原因");
        }
        repair.setAuditStatus(result);
        repair.setAuditOpinion(req.getAuditOpinion());
        repair.setAuditTime(LocalDateTime.now());
        creditRepairMapper.updateById(repair);

        // 通过修复 → 联动上调该主体信用分，形成闭环
        if ("PASS".equals(result) && repair.getTargetId() != null) {
            if ("ENTERPRISE".equals(repair.getTargetType())) {
                creditEnterpriseService.addScoreBonus(repair.getTargetId(), PASS_BONUS);
            } else if ("PERSONAL".equals(repair.getTargetType())) {
                creditPersonalService.addScoreBonus(repair.getTargetId(), PASS_BONUS);
            }
        }
        return repair;
    }
}