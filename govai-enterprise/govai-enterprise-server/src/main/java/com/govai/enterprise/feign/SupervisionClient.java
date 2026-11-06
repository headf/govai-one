package com.govai.enterprise.feign;

import com.govai.enterprise.common.Result;
import com.govai.enterprise.dto.CreditVO;
import com.govai.enterprise.dto.SupervisionReportReq;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * 监管端 Feign 客户端(监管端 govai-supervision)
 * 企业端申报在提交/审批定稿时上报监管端,作为大屏/风险预警的真实数据源;
 * 申报前查自身信用(守信激励/失信受限)。
 * 监管端不在线时由 fallback 静默降级,不阻塞申报主链路。
 */
@FeignClient(name = "govai-supervision",
        url = "${govai.feign.supervision-url:http://localhost:8081}",
        fallback = SupervisionClientFallback.class)
public interface SupervisionClient {

    /** 上报一条申报到监管端(ent-view 已从监管端 JWT 拦截器排除) */
    @PostMapping("/api/v1/ent-view/approvals/report")
    Result<Void> report(@RequestBody SupervisionReportReq req);

    /** 查本企业信用档案(ent-view 免 token,靠 X-Enterprise-Id 头定位企业) */
    @GetMapping("/api/v1/ent-view/credit")
    Result<CreditVO> myCredit(@RequestHeader("X-Enterprise-Id") Long enterpriseId);

    /** 差评上报(星级<=2 时由评价服务调用),在监管端生成"差评整改工单",形成整改闭环 */
    @PostMapping("/api/v1/review-orders/report")
    Result<Void> reportReviewOrder(@RequestBody java.util.Map<String, Object> body);
}
