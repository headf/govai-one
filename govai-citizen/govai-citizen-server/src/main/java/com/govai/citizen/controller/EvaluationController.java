package com.govai.citizen.controller;

import com.govai.citizen.common.Result;
import com.govai.citizen.dto.EvaluationStatsVO;
import com.govai.citizen.dto.EvaluationSubmitReq;
import com.govai.citizen.dto.EvaluationVO;
import com.govai.citizen.service.EvaluationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/evaluations")
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluationService;

    @PostMapping
    public Result<Void> submit(@Valid @RequestBody EvaluationSubmitReq req) {
        evaluationService.submit(req);
        return Result.success();
    }

    @GetMapping("/my")
    public Result<List<EvaluationVO>> my() {
        return Result.success(evaluationService.my());
    }

    @GetMapping("/statistics")
    public Result<EvaluationStatsVO> statistics() {
        return Result.success(evaluationService.stats());
    }
}
