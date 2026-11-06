package com.govai.approval.feign;

import com.govai.approval.common.Result;
import com.govai.approval.dto.OcrReq;
import com.govai.approval.dto.OcrVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * 企业端 Feign 客户端
 *
 * 接口8：POST /api/v1/ocr/recognize  OCR识别
 * 业务场景：审批员上传证照材料 -> OCR 自动识别字段 -> 回填表单
 *
 * 另：审批定稿回调企业端(办结出证/驳回/退回补正)——审批端定稿后主动通知企业端
 */
@FeignClient(
        name = "govai-enterprise",
        url = "${feign.targets.enterprise:http://localhost:8084}",
        fallback = EnterpriseClientFallback.class
)
public interface EnterpriseClient {

    @PostMapping("/api/v1/ocr/recognize")
    Result<OcrVO> recognize(@RequestBody OcrReq req);

    /** 审批定稿回调:通知企业端按审批实例更新申报状态并做业务联动(出证等)。status 传企业端口径 PASSED/REJECTED/REPAIR。
     *  返回 data 为审批通过时企业端签发的证照号(未出证为 null),供审批端回写办结台账 */
    @PostMapping("/api/v1/ent-apply/approval-result-by-instance")
    Result<String> notifyApprovalResult(@RequestParam("instanceId") Long instanceId,
                                        @RequestParam("status") String status,
                                        @RequestParam(value = "remark", required = false) String remark);

    /** 按审批实例查申报人上传的材料清单（审批端复核材料用；企业端不在线时降级为空列表） */
    @GetMapping("/api/v1/ent-apply/materials-by-instance/{instanceId}")
    Result<List<Map<String, Object>>> materialsByInstance(@PathVariable("instanceId") Long instanceId);
}
