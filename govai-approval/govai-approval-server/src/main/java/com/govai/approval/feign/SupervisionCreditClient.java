package com.govai.approval.feign;

import com.govai.approval.common.Result;
import com.govai.approval.dto.CreditVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 监管端企业信用 Feign 客户端
 *
 * 审批人在审批详情页查看申报企业信用等级(守信绿标/失信红标),作为审批辅助参考。
 * 监管端不在线时由 fallback 降级为"信用未知",不阻塞审批详情。
 */
@FeignClient(
        name = "govai-supervision-credit",
        url = "${feign.targets.supervision:http://localhost:8081}",
        fallback = SupervisionCreditClientFallback.class
)
public interface SupervisionCreditClient {

    /** 按统一社会信用代码查企业信用(审批实例的 applicantId 即企业信用代码) */
    @GetMapping("/api/v1/ent-view/credit/by-code")
    Result<CreditVO> byCode(@RequestParam("creditCode") String creditCode);
}
