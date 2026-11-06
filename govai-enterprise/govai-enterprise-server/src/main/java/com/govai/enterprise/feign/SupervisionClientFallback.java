package com.govai.enterprise.feign;

import com.govai.enterprise.common.Result;
import com.govai.enterprise.dto.CreditVO;
import com.govai.enterprise.dto.SupervisionReportReq;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 监管端 Feign 降级实现
 * 监管端不在线时静默返回,保证企业端申报主链路可独立演示、不因上报失败而中断。
 */
@Slf4j
@Component
public class SupervisionClientFallback implements SupervisionClient {

    @Override
    public Result<Void> report(SupervisionReportReq req) {
        log.debug("[Fallback] 监管端不可用,跳过申报上报 instanceId={}", req == null ? null : req.getInstanceId());
        return Result.success(null);
    }

    @Override
    public Result<CreditVO> myCredit(Long enterpriseId) {
        // fail-open:信用未知,不做失信拦截(演示需三端齐开才看到完整信用语义)
        log.debug("[Fallback] 监管端不可用,信用查询降级 enterpriseId={}", enterpriseId);
        return Result.success(null);
    }

    @Override
    public Result<Void> reportReviewOrder(java.util.Map<String, Object> body) {
        log.debug("[Fallback] 监管端不可用,跳过差评上报 star={}", body == null ? null : body.get("starRating"));
        return Result.success(null);
    }
}
