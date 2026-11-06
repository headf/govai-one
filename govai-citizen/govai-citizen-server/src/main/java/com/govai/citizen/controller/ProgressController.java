package com.govai.citizen.controller;

import com.govai.citizen.common.Result;
import com.govai.citizen.dto.ProgressVO;
import com.govai.citizen.service.ProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressService progressService;

    /**
     * 查询申报审批进度(通过Feign调审批端, 审批端不在线时返回Mock时间线)
     */
    @GetMapping("/records/{id}/progress")
    public Result<ProgressVO> progress(@PathVariable("id") Long recordId) {
        return Result.success(progressService.getProgress(recordId));
    }
}
