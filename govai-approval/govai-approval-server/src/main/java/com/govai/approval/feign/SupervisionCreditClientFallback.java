package com.govai.approval.feign;

import com.govai.approval.common.Result;
import com.govai.approval.dto.CreditVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 监管端企业信用 Feign 降级
 *
 * 监管端不可用时返回空信用(信用未知),不阻塞审批详情展示。
 */
@Slf4j
@Component
public class SupervisionCreditClientFallback implements SupervisionCreditClient {

    @Override
    public Result<CreditVO> byCode(String creditCode) {
        log.debug("[Fallback] 监管端不可用,信用查询降级 creditCode={}", creditCode);
        return Result.success(null);
    }
}
