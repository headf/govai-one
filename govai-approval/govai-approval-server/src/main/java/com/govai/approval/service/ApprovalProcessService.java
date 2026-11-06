package com.govai.approval.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.govai.approval.common.BusinessException;
import com.govai.approval.common.Constants;
import com.govai.approval.common.PageResult;
import com.govai.approval.common.UserContextHolder;
import com.govai.approval.dto.*;
import com.govai.approval.entity.*;
import com.govai.approval.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 流程引擎核心 Service
 *
 * 三大职责：
 * 1. 提交审批 -> 创建实例 + 启动首个环节任务
 * 2. 任务流转 -> PASS 进入下一环节 / REJECT 终止 / RETURN 退回上一环节
 * 3. 进度查询 -> 拼装环节状态
 *
 * 同时联动：规则引擎自动审核 / AI辅助 / 审计日志 / RabbitMQ 推送监管端
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalProcessService extends ServiceImpl<ApprovalInstanceMapper, ApprovalInstance> {

    private final ApprovalProcessTemplateMapper templateMapper;
    private final ApprovalProcessNodeMapper nodeMapper;
    private final ApprovalTaskMapper taskMapper;
    private final AuditLogMapper auditLogMapper;
    private final RuleEngineService ruleEngineService;
    private final AiAssistService aiAssistService;
    private final ApprovalMqProducer mqProducer;
    private final com.govai.approval.feign.EnterpriseClient enterpriseClient;
    private final com.govai.approval.feign.CitizenClient citizenClient;
    private final NotifyRetryService notifyRetryService;

    /**
     * 接口4：提交审批
     */
    @Transactional(rollbackFor = Exception.class)
    public SubmitVO submit(SubmitReq req) {
        // 1. 查流程模板
        ApprovalProcessTemplate template = templateMapper.selectOne(
                new LambdaQueryWrapper<ApprovalProcessTemplate>()
                        .eq(ApprovalProcessTemplate::getTemplateCode, req.getTemplateCode())
                        .eq(ApprovalProcessTemplate::getStatus, 1));
        if (template == null) {
            throw new BusinessException(404, "流程模板不存在或已停用: " + req.getTemplateCode());
        }

        // 2. 查首个环节
        List<ApprovalProcessNode> nodes = nodeMapper.selectList(
                new LambdaQueryWrapper<ApprovalProcessNode>()
                        .eq(ApprovalProcessNode::getTemplateId, template.getId())
                        .orderByAsc(ApprovalProcessNode::getSortOrder));
        if (CollectionUtils.isEmpty(nodes)) {
            throw new BusinessException("流程模板未配置环节: " + template.getTemplateName());
        }
        ApprovalProcessNode startNode = nodes.get(0);

        // 3. 创建审批实例
        ApprovalInstance instance = new ApprovalInstance();
        instance.setTemplateId(template.getId());
        instance.setMatterId(req.getMatterId());
        instance.setMatterName(StringUtils.hasText(req.getMatterName()) ? req.getMatterName() : template.getTemplateName());
        instance.setApplicantName(req.getApplicantName());
        instance.setApplicantId(req.getApplicantId());
        instance.setApplicantType(req.getApplicantType());
        instance.setFormData(toJson(req.getFormData()));
        instance.setStatus(Constants.INSTANCE_PENDING);
        instance.setSubmitTime(LocalDateTime.now());
        instance.setCurrentNodeId(startNode.getId());
        save(instance);

        // 4. 写审计日志 - 提交
        writeAuditLog(instance.getId(), Constants.OP_SUBMIT, "提交申请", null, Constants.INSTANCE_PENDING);

        // 5. 规则引擎自动审核（若该事项配置了规则集）
        RuleResultVO ruleResult = ruleEngineService.checkByMatterType(template.getMatterType(), req.getFormData());

        // 6. 根据规则结果决定流转
        // 材料存疑（材料名与上传文件名不符等）时，即使规则全过也不直达办结，转人工受理核对
        boolean materialSuspect = req.getFormData() != null
                && Boolean.TRUE.equals(req.getFormData().get("__materialSuspect"));
        String status;
        String currentNodeName = startNode.getNodeName();
        if (Constants.RULE_PASS.equals(ruleResult.getResult()) && !materialSuspect) {
            // 规则引擎全通过：不再"即办件自动办结"，而是快进到办结(出证)岗人工复核出证——
            // 由最后发证岗确认"规则核验已通过"后 PASS 才真正办结(规则只做辅助结论,终审权留人)
            ApprovalProcessNode endNode = nodes.stream()
                    .filter(n -> Constants.NODE_END.equals(n.getNodeType()))
                    .reduce((a, b) -> b) // 取最后一个 END 环节
                    .orElse(null);
            if (endNode != null) {
                instance.setSettleMode("ISSUE_REVIEW");
                createTask(instance.getId(), endNode);
                instance.setCurrentNodeId(endNode.getId());
                instance.setStatus(Constants.INSTANCE_APPROVING);
                currentNodeName = endNode.getNodeName() + "(规则已通过·待复核出证)";
                status = Constants.INSTANCE_APPROVING;
                writeAuditLog(instance.getId(), Constants.OP_APPROVE,
                        "AI自动审核(规则引擎)全通过,快进办结出证岗人工复核办结", Constants.INSTANCE_PENDING, Constants.INSTANCE_APPROVING);
            } else {
                // 模板无 END 环节兜底:回退为旧行为,按人工首环节流转
                createTask(instance.getId(), startNode);
                instance.setStatus(Constants.INSTANCE_APPROVING);
                status = Constants.INSTANCE_APPROVING;
            }
        } else if (Constants.RULE_PASS.equals(ruleResult.getResult())) {
            // 规则全过，但材料存疑（材料名与上传文件名不符等）→ 不直达办结，转人工受理岗核对材料
            createTask(instance.getId(), startNode);
            instance.setStatus(Constants.INSTANCE_APPROVING);
            currentNodeName = startNode.getNodeName() + "(规则已通过·材料待人工核对)";
            status = Constants.INSTANCE_APPROVING;
            writeAuditLog(instance.getId(), Constants.OP_APPROVE,
                    "AI规则校验全通过，但材料与上传文件不符/存疑，转人工受理岗核对材料",
                    Constants.INSTANCE_PENDING, Constants.INSTANCE_APPROVING);
        } else if (Constants.RULE_FAIL.equals(ruleResult.getResult())) {
            // 规则不通过 -> 走人工最终判定（仍然创建任务，让审批人确认）
            createTask(instance.getId(), startNode);
            instance.setStatus(Constants.INSTANCE_APPROVING);
            status = Constants.INSTANCE_APPROVING;
        } else {
            // TO_MANUAL 转人工 -> 创建首环节任务
            createTask(instance.getId(), startNode);
            instance.setStatus(Constants.INSTANCE_APPROVING);
            status = Constants.INSTANCE_APPROVING;
        }
        updateById(instance);

        // 7. 人工流转的申报:事务提交后后台异步预生成一次 AI 审核意见,审批人打开详情直接读取、无需等待现算
        if (Constants.INSTANCE_APPROVING.equals(status)) {
            final Long instId = instance.getId();
            final RuleResultVO ruleForAi = ruleResult;
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    // 不阻塞原请求线程:起独立线程后台生成(无真实api-key时走Mock,同样落库)
                    Thread t = new Thread(() -> {
                        try {
                            aiAssistService.generateApprovalOpinion(instId, ruleForAi);
                            log.info("AI意见后台预生成完成 instanceId={}", instId);
                        } catch (Exception e) {
                            log.warn("AI意见后台预生成失败(忽略,详情页仍可手动生成) instanceId={} err={}",
                                    instId, e.getMessage());
                        }
                    }, "ai-pre-gen-" + instId);
                    t.setDaemon(true);
                    t.start();
                }
            });
        }

        // 8. 推送审批数据到监管端（通过 RabbitMQ）
        mqProducer.pushApprovalRecord(buildPushDTO(instance, status));

        // 9. 返回结果
        SubmitVO vo = new SubmitVO();
        vo.setInstanceId(instance.getId());
        vo.setStatus(status);
        vo.setCurrentNodeName(currentNodeName);
        vo.setRuleResult(ruleResult.getResult());
        log.info("提交审批成功 instanceId={} status={} rule={}", instance.getId(), status, ruleResult.getResult());
        return vo;
    }

    /**
     * 接口5：查询审批进度
     */
    public ProgressVO getProgress(Long instanceId) {
        ApprovalInstance instance = getById(instanceId);
        if (instance == null) {
            throw new BusinessException(404, "审批实例不存在: " + instanceId);
        }
        // 查环节
        List<ApprovalProcessNode> nodes = nodeMapper.selectList(
                new LambdaQueryWrapper<ApprovalProcessNode>()
                        .eq(ApprovalProcessNode::getTemplateId, instance.getTemplateId())
                        .orderByAsc(ApprovalProcessNode::getSortOrder));
        // 查任务（按创建时间）
        List<ApprovalTask> tasks = taskMapper.selectList(
                new LambdaQueryWrapper<ApprovalTask>()
                        .eq(ApprovalTask::getInstanceId, instanceId)
                        .orderByAsc(ApprovalTask::getCreateTime));

        ProgressVO vo = new ProgressVO();
        vo.setInstanceId(instanceId);
        vo.setStatus(instance.getStatus());
        vo.setSettleMode(instance.getSettleMode());
        vo.setSubmitTime(instance.getSubmitTime());
        vo.setFinishTime(instance.getFinishTime());

        // 找到当前环节（若 currentNodeId 为空或不匹配，则取第一个环节）
        Long currentNodeId = instance.getCurrentNodeId();
        ApprovalProcessNode currentNode = nodes.stream()
                .filter(n -> Objects.equals(n.getId(), currentNodeId))
                .findFirst()
                .orElse(nodes.isEmpty() ? null : nodes.get(0));
        String currentName;
        if (Constants.INSTANCE_APPROVED.equals(instance.getStatus())
                && "AUTO".equals(instance.getSettleMode())) {
            // 即办件·系统自动办结：无进行中人工环节
            currentName = "即办件·系统自动办结";
        } else {
            currentName = currentNode != null ? currentNode.getNodeName() : "已办结";
        }
        vo.setCurrentNode(currentName);
        Long effectiveCurrentNodeId = currentNode != null ? currentNode.getId() : null;

        // 拼装每个环节的进度状态
        List<ProgressVO.NodeProgress> nodeProgresses = new ArrayList<>();
        String operatorRole = UserContextHolder.currentRole();
        for (ApprovalProcessNode node : nodes) {
            ProgressVO.NodeProgress np = new ProgressVO.NodeProgress();
            np.setName(node.getNodeName());
            np.setAssigneeRole(node.getAssigneeRole());
            // 该环节的任务:优先取进行中(PENDING)的任务(退回重办等场景可能同环节多任务),否则取最近一条
            ApprovalTask matchedTask = tasks.stream()
                    .filter(t -> Objects.equals(t.getNodeId(), node.getId()))
                    .filter(t -> Constants.TASK_PENDING.equals(t.getTaskStatus()))
                    .findFirst()
                    .orElse(tasks.stream()
                            .filter(t -> Objects.equals(t.getNodeId(), node.getId()))
                            .findFirst().orElse(null));
            if (matchedTask != null) {
                if (Constants.TASK_DONE.equals(matchedTask.getTaskStatus())) {
                    np.setStatus("DONE");
                } else if (Constants.TASK_PENDING.equals(matchedTask.getTaskStatus())) {
                    np.setStatus(Objects.equals(node.getId(), effectiveCurrentNodeId) ? "CURRENT" : "PENDING");
                } else {
                    np.setStatus("CANCEL");
                }
                np.setTaskId(matchedTask.getId());
                np.setAssignee(matchedTask.getAssigneeUserName());
                np.setHandleTime(matchedTask.getHandleTime());
                np.setApproveResult(matchedTask.getApproveResult());
                np.setApproveOpinion(matchedTask.getApproveOpinion());
                np.setHandleDetail(matchedTask.getHandleDetail());
                // 该环节待办是否归当前登录人岗位处理(岗位快照比对,决定前端是否显示操作区)
                np.setCanOperate(Constants.TASK_PENDING.equals(matchedTask.getTaskStatus())
                        && Objects.equals(matchedTask.getProcessRoleCode(), operatorRole));
            } else {
                // 该环节没任务 -> 当前登录人不可操作
                np.setCanOperate(false);
                // 该环节没任务 -> 按环节顺序与实例状态判断
                if (Constants.INSTANCE_APPROVED.equals(instance.getStatus())) {
                    np.setStatus("DONE"); // 已通过：所有环节完成
                } else if (Constants.INSTANCE_REJECTED.equals(instance.getStatus())
                        || Constants.INSTANCE_RETURNED.equals(instance.getStatus())) {
                    // 已拒绝/退回：当前环节及之前完成，之后取消
                    int currentSort = nodes.stream()
                            .filter(n -> Objects.equals(n.getId(), effectiveCurrentNodeId))
                            .map(ApprovalProcessNode::getSortOrder)
                            .findFirst().orElse(Integer.MAX_VALUE);
                    np.setStatus(node.getSortOrder() <= currentSort ? "DONE" : "CANCEL");
                } else {
                    // 审批中/待受理：当前环节之前完成，当前进行中，之后待处理
                    int currentSort = nodes.stream()
                            .filter(n -> Objects.equals(n.getId(), effectiveCurrentNodeId))
                            .map(ApprovalProcessNode::getSortOrder)
                            .findFirst().orElse(0);
                    if (node.getSortOrder() < currentSort) {
                        np.setStatus("DONE");
                    } else if (node.getSortOrder() == currentSort) {
                        np.setStatus("CURRENT");
                    } else {
                        np.setStatus("PENDING");
                    }
                }
            }
            nodeProgresses.add(np);
        }
        vo.setNodes(nodeProgresses);
        return vo;
    }

    /**
     * 处理审批任务（PASS/REJECT/RETURN）
     */
    @Transactional(rollbackFor = Exception.class)
    public SubmitVO handleTask(HandleTaskReq req, Long operatorId, String operatorName) {
        ApprovalTask task = taskMapper.selectById(req.getTaskId());
        if (task == null) {
            throw new BusinessException(404, "审批任务不存在");
        }
        if (!Constants.TASK_PENDING.equals(task.getTaskStatus())) {
            throw new BusinessException("任务已处理，请勿重复操作");
        }
        ApprovalInstance instance = getById(task.getInstanceId());
        if (instance == null) {
            throw new BusinessException(404, "审批实例不存在");
        }
        // 岗位鉴权：仅该环节所属岗位可办理（多岗位分离硬防线，前端按钮显隐仅是体验层）
        String operatorRole = UserContextHolder.currentRole();
        if (task.getProcessRoleCode() != null && !task.getProcessRoleCode().equals(operatorRole)) {
            throw new BusinessException(403, "该环节需 " + roleLabel(task.getProcessRoleCode()) + " 办理，当前账号无权操作");
        }
        // 办结/出证岗只可"出证办结"或"退回终审复核"，不可直接否决（终审已批准）
        if (Constants.ROLE_ISSUE.equals(task.getProcessRoleCode())
                && Constants.RESULT_REJECT.equals(req.getApproveResult())) {
            throw new BusinessException("办结环节不可驳回，请退回终审复核或联系终审负责人");
        }
        String beforeStatus = instance.getStatus();

        // 办理即认领(CAS)：仅当任务仍为 PENDING 时才置 DONE+认领，并发下后到者被拒，避免同一任务被两人办理
        task.setTaskStatus(Constants.TASK_DONE);
        task.setApproveResult(req.getApproveResult());
        task.setApproveOpinion(req.getApproveOpinion());
        task.setHandleDetail(req.getHandleDetail());
        task.setHandleTime(LocalDateTime.now());
        task.setAssigneeUserId(operatorId);
        task.setAssigneeUserName(operatorName);
        int claimed = taskMapper.update(task, new LambdaQueryWrapper<ApprovalTask>()
                .eq(ApprovalTask::getId, task.getId())
                .eq(ApprovalTask::getTaskStatus, Constants.TASK_PENDING));
        if (claimed == 0) {
            throw new BusinessException("任务已被他人办理，请刷新待办");
        }

        // 根据审批结果流转
        switch (req.getApproveResult()) {
            case Constants.RESULT_PASS  -> doPass(instance, task, req, operatorId, operatorName, beforeStatus);
            case Constants.RESULT_REJECT -> doReject(instance, req, operatorId, operatorName, beforeStatus);
            case Constants.RESULT_RETURN -> doReturn(instance, task, req, operatorId, operatorName, beforeStatus);
            default -> throw new BusinessException("不支持的审批结果: " + req.getApproveResult());
        }
        updateById(instance);
        writeAuditLog(instance.getId(), Constants.OP_APPROVE,
                req.getApproveOpinion() + (Boolean.TRUE.equals(req.getAdoptAiAdvice()) ? "(采纳AI建议)" : ""),
                beforeStatus, instance.getStatus());

        // 推送最新状态到监管端
        mqProducer.pushApprovalRecord(buildPushDTO(instance, instance.getStatus()));

        // 审批定稿(终态)时回调企业端,使其更新申报状态并做业务联动(办结出证/驳回/退回补正)
        String issuedNo = notifyEnterpriseOnFinal(instance, req);
        // 企业端出证后回传证照号,回写当前办结任务,保持审批端台账与企业端证照号一致
        if (issuedNo != null && Constants.RESULT_PASS.equals(req.getApproveResult())) {
            writeIssuedLicenseNo(task, issuedNo);
        }
        // 审批定稿(终态)时同步通知群众端: 本地状态 + 站内信(办结/驳回/退回实时可感), 含退回补正意见
        notifyCitizenOnFinal(instance, req.getApproveOpinion());

        SubmitVO vo = new SubmitVO();
        vo.setInstanceId(instance.getId());
        vo.setStatus(instance.getStatus());
        return vo;
    }

    /**
     * 同类办件参考(智能审批侧栏): 同一事项的历史终态件(办结/驳回/退回)聚合与最近结论,
     * 给审批员"知识图谱关联"式参考, 提升结论一致性。
     */
    public Map<String, Object> similarInstances(Long instanceId) {
        Map<String, Object> res = new LinkedHashMap<>();
        ApprovalInstance cur = baseMapper.selectById(instanceId);
        if (cur == null) {
            res.put("matterName", null);
            return res;
        }
        res.put("matterName", cur.getMatterName());
        LambdaQueryWrapper<ApprovalInstance> same = new LambdaQueryWrapper<ApprovalInstance>()
                .eq(ApprovalInstance::getMatterName, cur.getMatterName())
                .in(ApprovalInstance::getStatus, List.of(
                        Constants.INSTANCE_APPROVED, Constants.INSTANCE_REJECTED, Constants.INSTANCE_RETURNED));
        res.put("total", baseMapper.selectCount(same.clone()));
        res.put("passed", baseMapper.selectCount(same.clone()
                .eq(ApprovalInstance::getStatus, Constants.INSTANCE_APPROVED)));
        res.put("rejected", baseMapper.selectCount(same.clone()
                .eq(ApprovalInstance::getStatus, Constants.INSTANCE_REJECTED)));
        res.put("returned", baseMapper.selectCount(same.clone()
                .eq(ApprovalInstance::getStatus, Constants.INSTANCE_RETURNED)));
        List<Map<String, Object>> latest = baseMapper.selectList(same.clone()
                        .ne(ApprovalInstance::getId, instanceId)
                        .orderByDesc(ApprovalInstance::getFinishTime)
                        .last("limit 6"))
                .stream().map(x -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", x.getId());
                    m.put("applicantName", x.getApplicantName());
                    m.put("status", x.getStatus());
                    m.put("finishTime", x.getFinishTime() == null ? null : x.getFinishTime().toString());
                    return m;
                }).collect(Collectors.toList());
        res.put("latest", latest);
        return res;
    }

    /**
     * 实例进入终态(办结/驳回/退回)时通知群众端: 由群众端同步本地状态并生成站内信。
     * 仅终态触发; 失败仅告警不阻断审批主链路。
     */
    private void notifyCitizenOnFinal(com.govai.approval.entity.ApprovalInstance instance, String opinion) {
        String st = instance.getStatus();
        if (instance.getId() == null
                || (!Constants.INSTANCE_APPROVED.equals(st)
                && !Constants.INSTANCE_REJECTED.equals(st)
                && !Constants.INSTANCE_RETURNED.equals(st))) {
            return;
        }
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("instanceId", instance.getId());
            body.put("status", st);
            body.put("finishTime", instance.getFinishTime() == null ? null : instance.getFinishTime().toString());
            body.put("opinion", opinion);
            citizenClient.notifyCitizenStatus(body);
            log.info("已通知群众端审批定稿 instanceId={} status={}", instance.getId(), st);
        } catch (Exception e) {
            // 群众端不可用/回调异常不阻断审批;登记待重试,避免"审批已办结、群众端状态不更新"的跨端不一致
            log.warn("通知群众端审批定稿失败,已登记重试 instanceId={} err={}", instance.getId(), e.getMessage());
            notifyRetryService.recordFailure("CIT", instance.getId(), st, opinion, e.getMessage());
        }
    }

    /**
     * 实例进入终态(APPROVED/REJECTED/RETURNED)时,通知企业端(异步容错,失败不影响审批主链路)。
     * 状态映射: APPROVED->PASSED(办结出证) / REJECTED->REJECTED / RETURNED->REPAIR(退回补正)
     * @return 企业端签发的证照号(审批通过出证时返回;未出证/未到终态返回 null),供回写办结台账
     */
    private String notifyEnterpriseOnFinal(com.govai.approval.entity.ApprovalInstance instance,
                                           HandleTaskReq req) {
        return notifyEnterpriseOnFinal(instance, req == null ? null : req.getApproveOpinion());
    }

    private String notifyEnterpriseOnFinal(com.govai.approval.entity.ApprovalInstance instance, String opinion) {
        String entStatus = switch (instance.getStatus()) {
            case Constants.INSTANCE_APPROVED -> "PASSED";
            case Constants.INSTANCE_REJECTED -> "REJECTED";
            case Constants.INSTANCE_RETURNED -> "REPAIR";
            default -> null; // 仍在审批中(APPROVING),不回调
        };
        if (entStatus == null || instance.getId() == null) {
            return null;
        }
        try {
            com.govai.approval.common.Result<String> resp = enterpriseClient.notifyApprovalResult(instance.getId(), entStatus, opinion);
            String issuedNo = resp == null ? null : resp.getData();
            log.info("已通知企业端审批定稿 instanceId={} status={} 回传证照号={}", instance.getId(), entStatus, issuedNo);
            return issuedNo;
        } catch (Exception e) {
            // 企业端不可用/回调异常不阻断审批;登记待重试,避免"审批已办结、企业端状态不更新"的跨端不一致
            log.warn("通知企业端审批定稿失败,已登记重试 instanceId={} err={}", instance.getId(), e.getMessage());
            notifyRetryService.recordFailure("ENT", instance.getId(), entStatus, opinion, e.getMessage());
            return null;
        }
    }

    /** PASS：非 END 环节流转到下一环节(含流转到办结 END 建办结岗任务)；当前环节本身是 END(办结)时置通过终态 */
    private void doPass(ApprovalInstance instance, ApprovalTask task, HandleTaskReq req,
                        Long operatorId, String operatorName, String beforeStatus) {
        ApprovalProcessNode currentNode = nodeMapper.selectById(task.getNodeId());
        if (Constants.NODE_END.equals(currentNode.getNodeType())) {
            // 办结环节办理人 PASS -> 真正办结(终态),回调企业端由此刻触发
            instance.setStatus(Constants.INSTANCE_APPROVED);
            instance.setFinishTime(LocalDateTime.now());
            instance.setCurrentNodeId(null);
            return;
        }
        Long nextNodeId = currentNode.getNextNodeId();
        if (nextNodeId == null) {
            // 非 END 环节但无后续节点(模板末节点非 END 的兜底) -> 直接办结
            instance.setStatus(Constants.INSTANCE_APPROVED);
            instance.setFinishTime(LocalDateTime.now());
            instance.setCurrentNodeId(null);
            return;
        }
        ApprovalProcessNode nextNode = nodeMapper.selectById(nextNodeId);
        instance.setCurrentNodeId(nextNodeId);
        instance.setStatus(Constants.INSTANCE_APPROVING);
        // 创建下一环节任务(若下一环节是办结 END,则为办结岗生成任务)
        createTask(instance.getId(), nextNode);
    }

    /** REJECT：终止，状态置 REJECTED */
    private void doReject(ApprovalInstance instance, HandleTaskReq req,
                          Long operatorId, String operatorName, String beforeStatus) {
        instance.setStatus(Constants.INSTANCE_REJECTED);
        instance.setFinishTime(LocalDateTime.now());
    }

    /** RETURN：退回上一环节（找前驱节点） */
    private void doReturn(ApprovalInstance instance, ApprovalTask task, HandleTaskReq req,
                          Long operatorId, String operatorName, String beforeStatus) {
        // 找前驱节点：当前环节之前的那个环节
        List<ApprovalProcessNode> nodes = nodeMapper.selectList(
                new LambdaQueryWrapper<ApprovalProcessNode>()
                        .eq(ApprovalProcessNode::getTemplateId, instance.getTemplateId())
                        .orderByAsc(ApprovalProcessNode::getSortOrder));
        ApprovalProcessNode current = nodes.stream()
                .filter(n -> Objects.equals(n.getId(), task.getNodeId()))
                .findFirst().orElse(null);
        if (current == null) {
            instance.setStatus(Constants.INSTANCE_RETURNED);
            instance.setFinishTime(LocalDateTime.now());
            return;
        }
        int idx = nodes.indexOf(current);
        if (idx > 0) {
            ApprovalProcessNode prev = nodes.get(idx - 1);
            instance.setCurrentNodeId(prev.getId());
            instance.setStatus(Constants.INSTANCE_APPROVING);
            createTask(instance.getId(), prev);
        } else {
            // 没有上一环节 -> 退回到申请人补正
            instance.setStatus(Constants.INSTANCE_RETURNED);
            instance.setFinishTime(LocalDateTime.now());
        }
    }

    /** 创建审批任务：记录环节岗位快照；PENDING 任务办理人占位为该岗位名，办理完成时再落真实操作人 */
    private void createTask(Long instanceId, ApprovalProcessNode node) {
        ApprovalTask task = new ApprovalTask();
        task.setInstanceId(instanceId);
        task.setNodeId(node.getId());
        task.setProcessRoleCode(node.getAssigneeRole());
        task.setTaskStatus(Constants.TASK_PENDING);
        task.setAssigneeUserName(roleLabel(node.getAssigneeRole()));
        taskMapper.insert(task);
    }

    /** 审批员工作台 - 待办任务列表(按岗位过滤:当前登录人角色 == 任务所属环节岗位快照) */
    public PageResult<ApprovalTask> myTodoTasks(Long userId, String userName, int page, int size, String status,
                                                String nodeName) {
        String taskStatus = StringUtils.hasText(status) ? status : Constants.TASK_PENDING;
        String role = UserContextHolder.currentRole();
        LambdaQueryWrapper<ApprovalTask> wrapper = new LambdaQueryWrapper<ApprovalTask>()
                .eq(ApprovalTask::getTaskStatus, taskStatus)
                // 岗位待办：该环节岗位 == 当前登录人岗位；已办理历史按办理人本人可见
                .and(w -> {
                    w.eq(ApprovalTask::getProcessRoleCode, role)
                            .or()
                            .eq(ApprovalTask::getAssigneeUserId, userId);
                });
        // 按环节名筛选(受理/初审/决定/办结)
        if (StringUtils.hasText(nodeName)) {
            wrapper.apply("node_id IN (SELECT id FROM approval_process_node WHERE node_name = {0})", nodeName);
        }
        wrapper.orderByDesc(ApprovalTask::getCreateTime);
        Page<ApprovalTask> p = taskMapper.selectPage(new Page<>(page, size), wrapper);
        // 填充环节名(受理/初审/决定/办结),供前端分组/筛选展示;任务数量不大,批量查一次
        List<ApprovalTask> tasks = p.getRecords();
        if (!CollectionUtils.isEmpty(tasks)) {
            List<Long> nodeIds = tasks.stream()
                    .map(ApprovalTask::getNodeId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            if (!nodeIds.isEmpty()) {
                Map<Long, String> nameById = nodeMapper.selectList(
                                new LambdaQueryWrapper<ApprovalProcessNode>().in(ApprovalProcessNode::getId, nodeIds))
                        .stream().collect(Collectors.toMap(ApprovalProcessNode::getId, ApprovalProcessNode::getNodeName));
                tasks.forEach(t -> t.setNodeName(nameById.get(t.getNodeId())));
            }
        }
        return PageResult.of(tasks, p.getTotal(), page, size);
    }

    /** 待办统计：当前登录岗位各环节(受理/初审/决定/办结)的待处理数,供工作台分组实时展示 */
    public List<Map<String, Object>> myTodoStats(Long userId) {
        String role = UserContextHolder.currentRole();
        List<ApprovalTask> pendings = taskMapper.selectList(new LambdaQueryWrapper<ApprovalTask>()
                .eq(ApprovalTask::getTaskStatus, Constants.TASK_PENDING)
                .eq(ApprovalTask::getProcessRoleCode, role));
        if (CollectionUtils.isEmpty(pendings)) {
            return new ArrayList<>();
        }
        // 该岗位可能对应多套模板,同名环节(受理/初审/决定/办结)有不同 nodeId,需按 nodeName 合并计数
        List<Long> nodeIds = pendings.stream().map(ApprovalTask::getNodeId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, String> nameByNodeId = nodeMapper.selectList(
                        new LambdaQueryWrapper<ApprovalProcessNode>().in(ApprovalProcessNode::getId, nodeIds))
                .stream().collect(Collectors.toMap(ApprovalProcessNode::getId, ApprovalProcessNode::getNodeName,
                        (a, b) -> a));
        Map<String, Long> countByName = new HashMap<>();
        pendings.forEach(t -> {
            String name = nameByNodeId.get(t.getNodeId());
            if (name != null) countByName.merge(name, 1L, Long::sum);
        });
        List<Map<String, Object>> result = new ArrayList<>();
        countByName.forEach((name, cnt) -> {
            Map<String, Object> m = new HashMap<>();
            m.put("nodeName", name);
            m.put("count", cnt);
            result.add(m);
        });
        // 固定按流程顺序排(受理<初审<决定<办结)
        result.sort(Comparator.comparingInt(m -> {
            String n = String.valueOf(m.get("nodeName"));
            return switch (n) {
                case "受理" -> 1;
                case "初审" -> 2;
                case "决定" -> 3;
                case "办结" -> 4;
                default -> 9;
            };
        }));
        return result;
    }

    /** 实例分页查询 */
    public PageResult<ApprovalInstance> pageInstances(String status, String applicantName, int page, int size) {
        LambdaQueryWrapper<ApprovalInstance> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) wrapper.eq(ApprovalInstance::getStatus, status);
        if (StringUtils.hasText(applicantName)) wrapper.like(ApprovalInstance::getApplicantName, applicantName);
        wrapper.orderByDesc(ApprovalInstance::getCreateTime);
        Page<ApprovalInstance> p = page(new Page<>(page, size), wrapper);
        return PageResult.of(p.getRecords(), p.getTotal(), page, size);
    }

    // ============== 工具方法 ==============

    /** 岗位角色码 -> 中文名（用于任务占位/进度展示） */
    private String roleLabel(String role) {
        if (role == null) return "";
        return switch (role) {
            case Constants.ROLE_APPROVER -> "受理初审员";
            case Constants.ROLE_DECIDE -> "终审负责人";
            case Constants.ROLE_ISSUE -> "办结出证员";
            default -> role;
        };
    }

    /** 企业端回传证照号合并进任务 handle_detail(JSON),供审批台账/详情展示,保持两端号一致 */
    private void writeIssuedLicenseNo(ApprovalTask task, String licenseNo) {
        try {
            Map<String, Object> detail = new HashMap<>();
            if (StringUtils.hasText(task.getHandleDetail())) {
                detail.putAll(new com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue(task.getHandleDetail(), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}));
            }
            detail.put("licenseNo", licenseNo);
            task.setHandleDetail(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(detail));
            taskMapper.updateById(task);
            log.info("已回写证照号到办结任务 taskId={} licenseNo={}", task.getId(), licenseNo);
        } catch (Exception e) {
            log.warn("回写证照号到办结任务失败(忽略) taskId={} err={}", task.getId(), e.getMessage());
        }
    }

    private String toJson(Map<String, Object> formData) {
        if (formData == null) return null;
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(formData);
        } catch (Exception e) {
            return formData.toString();
        }
    }

    private void writeAuditLog(Long instanceId, String opType, String opinion, String before, String after) {
        AuditLog log = new AuditLog();
        log.setInstanceId(instanceId);
        log.setOperatorId(UserContextHolder.currentUserId());
        log.setOperatorName(UserContextHolder.currentUserName());
        log.setOperationType(opType);
        log.setOperationOpinion(opinion);
        log.setBeforeStatus(before);
        log.setAfterStatus(after);
        log.setOperationTime(LocalDateTime.now());
        auditLogMapper.insert(log);
    }

    private ApprovalRecordPushDTO buildPushDTO(ApprovalInstance instance, String status) {
        ApprovalRecordPushDTO dto = new ApprovalRecordPushDTO();
        dto.setInstanceId(instance.getId());
        dto.setMatterName(instance.getMatterName());
        dto.setApplicantName(instance.getApplicantName());
        dto.setApplicantType(instance.getApplicantType());
        dto.setApprovalStatus(status);
        dto.setSubmitTime(instance.getSubmitTime());
        dto.setFinishTime(instance.getFinishTime());
        if (instance.getSubmitTime() != null && instance.getFinishTime() != null) {
            double hours = Duration.between(instance.getSubmitTime(), instance.getFinishTime()).toMinutes() / 60.0;
            dto.setProcessDurationHours(Math.round(hours * 100) / 100.0);
        }
        return dto;
    }
}
