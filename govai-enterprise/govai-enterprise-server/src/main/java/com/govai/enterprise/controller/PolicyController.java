package com.govai.enterprise.controller;

import com.govai.enterprise.common.LoginUser;
import com.govai.enterprise.common.LoginUserContext;
import com.govai.enterprise.common.Result;
import com.govai.enterprise.entity.EntPolicyMatch;
import com.govai.enterprise.service.PolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 企业政策接口(政策精准推送 + 免申即享确认)
 */
@Tag(name = "企业政策")
@RestController
@RequestMapping("/api/v1/ent-policy")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyService policyService;

    @Operation(summary = "我的政策匹配列表")
    @GetMapping("/list")
    public Result<List<EntPolicyMatch>> list() {
        LoginUser user = LoginUserContext.require();
        return Result.success(policyService.listByEnterprise(user.getEnterpriseId()));
    }

    @Operation(summary = "免申即享确认")
    @PostMapping("/{id}/confirm")
    public Result<EntPolicyMatch> confirm(@PathVariable Long id) {
        LoginUser user = LoginUserContext.require();
        return Result.success(policyService.confirm(user.getEnterpriseId(), id));
    }
}
