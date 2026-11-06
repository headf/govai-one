package com.govai.citizen.controller;

import com.govai.citizen.common.Result;
import com.govai.citizen.dto.PolicyVO;
import com.govai.citizen.service.PolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/policies")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyService policyService;

    @GetMapping("/list")
    public Result<List<PolicyVO>> list() {
        return Result.success(policyService.list());
    }

    @GetMapping("/{id}")
    public Result<PolicyVO> detail(@PathVariable("id") Long id) {
        return Result.success(policyService.detail(id));
    }

    @PostMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable("id") Long id) {
        policyService.markRead(id);
        return Result.success();
    }
}
