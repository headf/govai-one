package com.govai.citizen.feign;

import com.govai.citizen.common.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * 审批端跨服务客户端 (govai-approval)
 * 开发阶段: 直连 localhost:8082 + Fallback Mock(审批端不在线也能跑通)
 * 第6周联调: 删掉 url 参数, 由 Nacos 服务发现路由
 */
@FeignClient(name = "govai-approval", url = "${govai.feign.approval-url:http://localhost:8082}",
        fallbackFactory = ApprovalClientFallbackFactory.class)
public interface ApprovalClient {

    @PostMapping("/api/v1/approval/submit")
    Result<Map<String, Object>> submit(@RequestBody Map<String, Object> req);

    @GetMapping("/api/v1/approval/progress/{instanceId}")
    Result<Map<String, Object>> progress(@PathVariable("instanceId") Long instanceId);

    @PostMapping("/api/v1/ai/chat")
    Result<Map<String, Object>> aiChat(@RequestBody Map<String, Object> req);
}
