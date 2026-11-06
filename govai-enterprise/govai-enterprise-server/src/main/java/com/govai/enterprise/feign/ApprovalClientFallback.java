package com.govai.enterprise.feign;

import com.govai.enterprise.common.Result;
import com.govai.enterprise.dto.ApprovalProgressResp;
import com.govai.enterprise.dto.ApprovalSubmitReq;
import com.govai.enterprise.dto.ApprovalSubmitResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 审批端 Feign 降级实现(开发阶段 Mock,骨架文档 7.2)
 * 审批端未启动时自动降级,返回模拟实例ID与进度,保证企业端申报主链路可独立演示
 */
@Slf4j
@Component
public class ApprovalClientFallback implements ApprovalClient {

    @Override
    public Result<ApprovalSubmitResp> submit(ApprovalSubmitReq req) {
        log.warn("[Fallback] 审批端不可用,返回 Mock 提交结果: instanceId=5001");
        ApprovalSubmitResp resp = new ApprovalSubmitResp();
        resp.setInstanceId(5001L);
        resp.setStatus("APPROVING");
        resp.setMock(true); // 标记为降级模拟结果,供调用方区分"真提交"与"本地模拟"
        return Result.success(resp);
    }

    @Override
    public Result<ApprovalProgressResp> progress(Long instanceId) {
        log.warn("[Fallback] 审批端不可用,返回 Mock 进度: instanceId={}", instanceId);
        String now = LocalDateTime.now().toString();
        ApprovalProgressResp resp = new ApprovalProgressResp();
        resp.setInstanceId(instanceId);
        resp.setStatus("APPROVING");
        resp.setCurrentNode("初审");
        resp.setSubmitTime(now);
        List<ApprovalProgressResp.Node> nodes = new ArrayList<>();
        ApprovalProgressResp.Node n1 = new ApprovalProgressResp.Node();
        n1.setName("提交"); n1.setStatus("DONE"); n1.setHandleTime(now);
        ApprovalProgressResp.Node n2 = new ApprovalProgressResp.Node();
        n2.setName("初审"); n2.setStatus("CURRENT"); n2.setAssignee("张审批");
        ApprovalProgressResp.Node n3 = new ApprovalProgressResp.Node();
        n3.setName("复审"); n3.setStatus("PENDING");
        ApprovalProgressResp.Node n4 = new ApprovalProgressResp.Node();
        n4.setName("办结"); n4.setStatus("PENDING");
        nodes.add(n1); nodes.add(n2); nodes.add(n3); nodes.add(n4);
        resp.setNodes(nodes);
        return Result.success(resp);
    }
}
