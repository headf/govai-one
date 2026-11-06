package com.govai.citizen.controller;

import com.govai.citizen.common.Result;
import com.govai.citizen.service.ExternalStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 跨服务内部回调(审批端审批定稿推送办结/驳回/退回状态), 无需群众端 JWT(见 WebMvcConfig exclude)
 */
@RestController
@RequestMapping("/api/v1/external")
@RequiredArgsConstructor
public class ExternalStatusController {

    private final ExternalStatusService externalStatusService;

    @PostMapping("/records/status")
    public Result<Void> recordStatus(@RequestBody(required = false) Map<String, Object> body) {
        externalStatusService.handleStatus(body);
        return Result.success();
    }
}
