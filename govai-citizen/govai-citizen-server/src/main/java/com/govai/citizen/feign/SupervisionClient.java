package com.govai.citizen.feign;

import com.govai.citizen.common.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * 监管端认证中心跨服务客户端 (govai-supervision, SSO)
 * mock-login=false 时群众端登录走监管端认证中心: 短信快捷登录拿统一 token(透传全链路)
 */
@FeignClient(name = "govai-supervision", url = "${govai.feign.supervision-url:http://localhost:8081}")
public interface SupervisionClient {

    /** 手机号+固定验证码快捷登录(CITIZEN), 返回统一 JWT(token.userId=监管端 sys_user.id) */
    @PostMapping("/api/v1/auth/sms-login")
    Result<Map<String, Object>> smsLogin(@RequestBody Map<String, Object> req);

    /** 办件上报(含公民身份关联), 写入监管端审批台账 approval_data_cache */
    @PostMapping("/api/v1/cit-view/approvals/report")
    Result<Map<String, Object>> reportApproval(@RequestBody Map<String, Object> body);

    /** 个人信用查询(按监管端 sys_user.id) */
    @GetMapping("/api/v1/cit-view/credit")
    Result<Map<String, Object>> personalCredit(@RequestParam("userId") Long userId);

    /** 差评上报(星级<=2 时由评价服务调用),在监管端生成"差评整改工单",形成整改闭环 */
    @PostMapping("/api/v1/review-orders/report")
    Result<Void> reportReviewOrder(@RequestBody Map<String, Object> body);
}
