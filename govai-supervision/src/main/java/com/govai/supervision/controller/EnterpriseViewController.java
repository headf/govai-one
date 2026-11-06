package com.govai.supervision.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.govai.supervision.common.BusinessException;
import com.govai.supervision.common.PageResult;
import com.govai.supervision.common.Result;
import com.govai.supervision.entity.CreditRepair;
import com.govai.supervision.entity.RiskAlert;
import com.govai.supervision.mapper.CreditRepairMapper;
import com.govai.supervision.mapper.RiskAlertMapper;
import com.govai.supervision.mq.ApprovalDataConsumer;
import com.govai.supervision.mq.ApprovalPushMessage;
import com.govai.supervision.service.CreditEnterpriseService;
import com.govai.supervision.entity.CreditEnterprise;
import com.govai.supervision.task.RiskScanTask;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 企业端视角只读接口(/api/v1/ent-view)
 * 供企业端查看"本企业"被监管情况：信用档案 / 风险预警 / 信用修复记录；并接收企业端真实申报上报。
 * 该前缀已从 JWT 拦截器排除，企业身份通过请求头 X-Enterprise-Id(enterprise_info.id) 承载。
 * 说明：演示系统简化鉴权——由企业端登录态 + 反向代理路径共同约束来源，不做完整 OAuth。
 */
@Slf4j
@Tag(name = "企业视角(ent-view)", description = "企业查看自身信用/预警/修复 + 申报上报")
@RestController
@RequestMapping("/api/v1/ent-view")
@RequiredArgsConstructor
public class EnterpriseViewController {

    private final CreditEnterpriseService creditEnterpriseService;
    private final RiskAlertMapper riskAlertMapper;
    private final CreditRepairMapper creditRepairMapper;
    private final ApprovalDataConsumer approvalDataConsumer;
    private final RiskScanTask riskScanTask;

    private static final String ENTERPRISE_TARGET = "ENTERPRISE";

    /** 从请求头取企业ID */
    private Long requireEnterpriseId(String enterpriseIdHeader) {
        if (enterpriseIdHeader == null || enterpriseIdHeader.isBlank()) {
            throw new BusinessException(400, "缺少企业标识(X-Enterprise-Id)");
        }
        try {
            return Long.valueOf(enterpriseIdHeader.trim());
        } catch (NumberFormatException e) {
            throw new BusinessException(400, "企业标识非法");
        }
    }

    /** 本企业信用档案 */
    @Operation(summary = "本企业信用档案")
    @GetMapping("/credit")
    public Result<CreditEnterprise> credit(@RequestHeader("X-Enterprise-Id") String enterpriseIdHeader) {
        Long enterpriseId = requireEnterpriseId(enterpriseIdHeader);
        return Result.success(creditEnterpriseService.getByEnterpriseId(enterpriseId));
    }

    /** 按统一社会信用代码查信用(审批端展示用;免 token 场景) */
    @Operation(summary = "按统一社会信用代码查询企业信用")
    @GetMapping("/credit/by-code")
    public Result<CreditEnterprise> creditByCode(@RequestParam String creditCode) {
        if (creditCode == null || creditCode.isBlank()) {
            throw new BusinessException(400, "缺少统一社会信用代码");
        }
        return Result.success(creditEnterpriseService.getByCreditCode(creditCode));
    }

    /** 本企业风险预警列表 */
    @Operation(summary = "本企业风险预警")
    @GetMapping("/alerts")
    public Result<PageResult<RiskAlert>> alerts(@RequestHeader("X-Enterprise-Id") String enterpriseIdHeader,
                                                @RequestParam(defaultValue = "1") long page,
                                                @RequestParam(defaultValue = "10") long size,
                                                @RequestParam(required = false) String handleStatus) {
        Long enterpriseId = requireEnterpriseId(enterpriseIdHeader);
        LambdaQueryWrapper<RiskAlert> wrapper = new LambdaQueryWrapper<RiskAlert>()
                .eq(RiskAlert::getTargetType, ENTERPRISE_TARGET)
                .eq(RiskAlert::getTargetId, enterpriseId)
                .eq(handleStatus != null && !handleStatus.isBlank(), RiskAlert::getHandleStatus, handleStatus)
                .orderByDesc(RiskAlert::getAlertTime);
        return Result.success(PageResult.of(riskAlertMapper.selectPage(new Page<>(page, size), wrapper)));
    }

    /** 本企业信用修复记录 */
    @Operation(summary = "本企业信用修复记录")
    @GetMapping("/repairs")
    public Result<PageResult<CreditRepair>> repairs(@RequestHeader("X-Enterprise-Id") String enterpriseIdHeader,
                                                    @RequestParam(defaultValue = "1") long page,
                                                    @RequestParam(defaultValue = "10") long size) {
        Long enterpriseId = requireEnterpriseId(enterpriseIdHeader);
        LambdaQueryWrapper<CreditRepair> wrapper = new LambdaQueryWrapper<CreditRepair>()
                .eq(CreditRepair::getTargetType, ENTERPRISE_TARGET)
                .eq(CreditRepair::getTargetId, enterpriseId)
                .orderByDesc(CreditRepair::getApplyTime);
        return Result.success(PageResult.of(creditRepairMapper.selectPage(new Page<>(page, size), wrapper)));
    }

    /**
     * 接收企业端真实申报上报(企业端提交/审批定稿时调用)
     * 复用 consumer.persist 的 instanceId 去重逻辑落 approval_data_cache，随后触发一次风险扫描。
     */
    @Operation(summary = "接收企业申报上报")
    @PostMapping("/approvals/report")
    public Result<Void> reportApproval(@RequestBody ApprovalPushMessage msg) {
        if (msg == null || msg.getEnterpriseId() == null) {
            throw new BusinessException(400, "上报缺少企业ID");
        }
        // 企业端 token 不入监管端用户体系，这里带默认企业类型
        if (msg.getApplicantType() == null) {
            msg.setApplicantType(ENTERPRISE_TARGET);
        }
        approvalDataConsumer.persist(msg);
        try {
            riskScanTask.scanRisk();
        } catch (Exception e) {
            log.warn("上报后风险扫描失败(不影响落库)", e);
        }
        return Result.<Void>success("上报成功", null);
    }
}
