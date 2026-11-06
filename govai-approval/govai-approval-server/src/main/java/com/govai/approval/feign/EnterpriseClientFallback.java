package com.govai.approval.feign;

import com.govai.approval.common.Result;
import com.govai.approval.dto.OcrReq;
import com.govai.approval.dto.OcrVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 企业端 Feign 降级（OCR 接口不可用时返回 Mock 识别结果）
 */
@Slf4j
@Component
public class EnterpriseClientFallback implements EnterpriseClient {

    @Override
    public Result<OcrVO> recognize(OcrReq req) {
        log.warn("企业端 OCR 不可用，返回 Mock 识别结果 type={}", req.getType());
        OcrVO vo = new OcrVO();
        Map<String, Object> fields = new HashMap<>();
        fields.put("enterpriseName", "Mock科技有限公司");
        fields.put("creditCode", "91110108MA01MOCK00");
        fields.put("legalPerson", "Mock法人");
        vo.setFields(fields);
        vo.setConfidence(0.0);
        return Result.success(vo);
    }

    @Override
    public Result<String> notifyApprovalResult(Long instanceId, String status, String remark) {
        // 企业端不在线时静默降级:审批主链路不因回调失败而回滚(监管端推送已另行完成)
        log.warn("企业端审批回调不可用，跳过(instanceId={} status={})", instanceId, status);
        return Result.success(null);
    }

    @Override
    public Result<List<Map<String, Object>>> materialsByInstance(Long instanceId) {
        log.warn("企业端不可用，申报材料查询降级为空列表(instanceId={})", instanceId);
        return Result.success(new java.util.ArrayList<>());
    }
}
