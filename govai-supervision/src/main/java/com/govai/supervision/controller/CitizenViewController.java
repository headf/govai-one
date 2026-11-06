package com.govai.supervision.controller;

import com.govai.supervision.common.BusinessException;
import com.govai.supervision.common.Result;
import com.govai.supervision.entity.CreditPersonal;
import com.govai.supervision.mq.ApprovalDataConsumer;
import com.govai.supervision.mq.ApprovalPushMessage;
import com.govai.supervision.service.CreditPersonalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公民(群众)端视角只读接口(/api/v1/cit-view)
 * 供群众端上报本人办件(补公民身份关联) 与 查询本人个人信用。
 * 与 /ent-view 同取舍：该前缀从 JWT 拦截器排除，公民身份由请求体/参数(phone/userId)自证，
 * 仅内网 + 群众端登录态约束来源，不做完整 OAuth（沿用现有架构约定）。
 */
@Slf4j
@Tag(name = "公民视角(cit-view)", description = "群众端上报办件 + 查询个人信用")
@RestController
@RequestMapping("/api/v1/cit-view")
@RequiredArgsConstructor
public class CitizenViewController {

    private final ApprovalDataConsumer approvalDataConsumer;
    private final CreditPersonalService creditPersonalService;

    /** 群众端真实申报上报：补全公民身份字段后写入审批台账(approval_data_cache)，按 instanceId 幂等 */
    @Operation(summary = "办件上报(公民)")
    @PostMapping("/approvals/report")
    public Result<Void> reportApproval(@RequestBody ApprovalPushMessage msg) {
        if (msg == null || msg.getInstanceId() == null) {
            throw new BusinessException(400, "上报缺少实例ID");
        }
        if (msg.getApplicantType() == null) {
            msg.setApplicantType("PERSONAL");
        }
        approvalDataConsumer.persist(msg);
        return Result.<Void>success("上报成功", null);
    }

    /** 个人信用查询(供群众端个人中心展示) */
    @Operation(summary = "个人信用查询")
    @GetMapping("/credit")
    public Result<CreditPersonal> credit(@RequestParam Long userId) {
        return Result.success(creditPersonalService.getByUserId(userId));
    }
}
