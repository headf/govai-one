package com.govai.enterprise.feign;

import com.govai.enterprise.dto.ApprovalProgressResp;
import com.govai.enterprise.dto.ApprovalSubmitReq;
import com.govai.enterprise.dto.ApprovalSubmitResp;
import com.govai.enterprise.common.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 审批端 Feign 客户端(契约接口4/5,提供方:审批端 govai-approval)
 * 开发阶段(第1-5周):直连 localhost:8082,配 fallback Mock,审批端不在线也能走通申报
 * 联调阶段(第6周):删掉 url,改由 Nacos 服务发现 govai-approval
 */
@FeignClient(name = "govai-approval",
        url = "${govai.feign.approval-url:http://localhost:8082}",
        fallback = ApprovalClientFallback.class)
public interface ApprovalClient {

    /**
     * 提交审批(契约接口4)
     */
    @PostMapping("/api/v1/approval/submit")
    Result<ApprovalSubmitResp> submit(@RequestBody ApprovalSubmitReq req);

    /**
     * 查审批进度(契约接口5)
     */
    @GetMapping("/api/v1/approval/progress/{instanceId}")
    Result<ApprovalProgressResp> progress(@PathVariable("instanceId") Long instanceId);
}
