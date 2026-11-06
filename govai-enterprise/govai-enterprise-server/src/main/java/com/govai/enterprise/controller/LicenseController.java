package com.govai.enterprise.controller;

import com.govai.enterprise.common.LoginUser;
import com.govai.enterprise.common.LoginUserContext;
import com.govai.enterprise.common.Result;
import com.govai.enterprise.entity.EnterpriseLicense;
import com.govai.enterprise.service.LicenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 企业证照接口
 */
@Tag(name = "企业证照")
@RestController
@RequestMapping("/api/v1/ent-license")
@RequiredArgsConstructor
public class LicenseController {

    private final LicenseService licenseService;

    @Operation(summary = "我的企业证照列表")
    @GetMapping("/list")
    public Result<List<EnterpriseLicense>> list() {
        LoginUser user = LoginUserContext.require();
        return Result.success(licenseService.listByEnterprise(user.getEnterpriseId()));
    }

    @Operation(summary = "证照到期提醒", description = "查询将在 days 天内到期(含已过期)的有效证照")
    @GetMapping("/expiring")
    public Result<List<EnterpriseLicense>> expiring(@RequestParam(defaultValue = "30") int days) {
        LoginUser user = LoginUserContext.require();
        return Result.success(licenseService.listExpiring(user.getEnterpriseId(), days));
    }
}
