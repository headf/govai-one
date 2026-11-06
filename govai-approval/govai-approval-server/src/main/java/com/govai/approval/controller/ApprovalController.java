package com.govai.approval.controller;

import com.govai.approval.common.PageResult;
import com.govai.approval.common.Result;
import com.govai.approval.common.UserContextHolder;
import com.govai.approval.dto.*;
import com.govai.approval.entity.ApprovalInstance;
import com.govai.approval.entity.ApprovalTask;
import com.govai.approval.feign.CitizenClient;
import com.govai.approval.feign.EnterpriseClient;
import com.govai.approval.feign.SupervisionCreditClient;
import com.govai.approval.service.ApprovalProcessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 审批 Controller
 *
 * 提供对外4个跨服务接口中的2个：
 *   接口4: POST /api/v1/approval/submit          提交审批
 *   接口5: GET  /api/v1/approval/progress/{id}   查审批进度
 *
 * 以及审批员工作台对内接口：
 *   待办任务列表 / 实例分页查询 / 处理任务 / 调用事项信息 / OCR识别
 */
@Tag(name = "审批管理")
@RestController
@Slf4j
@RequestMapping("/api/v1/approval")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalProcessService processService;
    private final CitizenClient citizenClient;
    private final EnterpriseClient enterpriseClient;
    private final SupervisionCreditClient supervisionCreditClient;

    @Operation(summary = "接口4-提交审批（群众端/企业端调用）")
    @PostMapping("/submit")
    public Result<SubmitVO> submit(@Valid @RequestBody SubmitReq req) {
        return Result.success(processService.submit(req));
    }

    @Operation(summary = "接口5-查询审批进度（群众端/企业端调用）")
    @GetMapping("/progress/{instanceId}")
    public Result<ProgressVO> progress(@PathVariable Long instanceId) {
        return Result.success(processService.getProgress(instanceId));
    }

    @Operation(summary = "处理审批任务（PASS/REJECT/RETURN）")
    @PostMapping("/task/handle")
    public Result<SubmitVO> handleTask(@Valid @RequestBody HandleTaskReq req) {
        return Result.success(processService.handleTask(req,
                UserContextHolder.currentUserId(),
                UserContextHolder.currentUserName()));
    }

    @Operation(summary = "我的待办任务")
    @GetMapping("/task/todo")
    public Result<PageResult<ApprovalTask>> myTodo(
            @RequestParam(defaultValue = "PENDING") String status,
            @RequestParam(required = false) String nodeName,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(processService.myTodoTasks(
                UserContextHolder.currentUserId(),
                UserContextHolder.currentUserName(), page, size, status, nodeName));
    }

    @Operation(summary = "待办统计：当前岗位各环节待处理数")
    @GetMapping("/task/todo-stats")
    public Result<java.util.List<java.util.Map<String, Object>>> myTodoStats() {
        return Result.success(processService.myTodoStats(UserContextHolder.currentUserId()));
    }

    @Operation(summary = "审批实例分页查询")
    @GetMapping("/instance/list")
    public Result<PageResult<ApprovalInstance>> pageInstances(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String applicantName,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(processService.pageInstances(status, applicantName, page, size));
    }

    @Operation(summary = "审批实例详情")
    @GetMapping("/instance/{id}")
    public Result<ApprovalInstance> getInstance(@PathVariable Long id) {
        ApprovalInstance instance = processService.getById(id);
        if (instance == null) return Result.fail(404, "审批实例不存在: " + id);
        // 企业申报:跨端查监管端信用等级,审批人参考(监管端离线降级为信用未知,不阻塞详情)
        if ("ENTERPRISE".equals(instance.getApplicantType())
                && instance.getApplicantId() != null && !instance.getApplicantId().isBlank()) {
            try {
                CreditVO credit = supervisionCreditClient.byCode(instance.getApplicantId()).getData();
                if (credit != null) {
                    instance.setCreditLevel(credit.getCreditLevel());
                    instance.setCreditScore(credit.getCreditScore());
                }
            } catch (Exception e) {
                // fallback 已兜底,此处再兜一次避免 feign 异常上抛
                log.warn("查企业信用失败,审批详情降级显示 instanceId={}", id, e);
            }
        }
        return Result.success(instance);
    }

    @Operation(summary = "同类办件参考(智能审批): 同一事项历史终态聚合与最近结论")
    @GetMapping("/instance/{id}/similar")
    public Result<java.util.Map<String, Object>> similar(@PathVariable Long id) {
        return Result.success(processService.similarInstances(id));
    }

    @Operation(summary = "申报材料清单(调用企业端接口,审批端复核材料用)")
    @GetMapping("/instance/{id}/materials")
    public Result<java.util.List<java.util.Map<String, Object>>> instanceMaterials(@PathVariable Long id) {
        return enterpriseClient.materialsByInstance(id);
    }

    @Operation(summary = "查事项信息（调用群众端接口3）")
    @GetMapping("/matter/{id}")
    public Result<MatterVO> getMatter(@PathVariable Long id) {
        return citizenClient.getMatter(id);
    }

    @Operation(summary = "OCR识别（调用企业端接口8）")
    @PostMapping("/ocr/recognize")
    public Result<OcrVO> recognize(@Valid @RequestBody OcrReq req) {
        return enterpriseClient.recognize(req);
    }
}
