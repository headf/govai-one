package com.govai.approval.feign;

import com.govai.approval.common.Result;
import com.govai.approval.dto.MatterVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * 群众端 Feign 客户端
 *
 * 接口3：GET /api/v1/matters/{id}  查事项信息
 * 接口4：POST /api/v1/external/records/status  审批定稿(办结/驳回/退回)时通知群众端
 * 开发阶段用 url 直连(localhost:8083)，第6周联调改为 Nacos 服务发现（删 url 参数）
 */
@FeignClient(
        name = "govai-citizen",
        url = "${feign.targets.citizen:http://localhost:8083}",
        fallback = CitizenClientFallback.class
)
public interface CitizenClient {

    @GetMapping("/api/v1/matters/{id}")
    Result<MatterVO> getMatter(@PathVariable("id") Long id);

    /**
     * 审批定稿通知群众端: 同步本地申报状态并生成站内信(APPROVAL)
     */
    @PostMapping("/api/v1/external/records/status")
    Result<String> notifyCitizenStatus(@RequestBody Map<String, Object> body);
}
