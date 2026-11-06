package com.govai.enterprise.controller;

import com.govai.enterprise.common.BusinessException;
import com.govai.enterprise.common.LoginUserContext;
import com.govai.enterprise.common.Result;
import com.govai.enterprise.dto.EnterpriseBriefVO;
import com.govai.enterprise.dto.LegalPersonUpdateReq;
import com.govai.enterprise.entity.EnterpriseInfo;
import com.govai.enterprise.service.EnterpriseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 企业信息管理接口(前缀 ent-enterprise)
 */
@Tag(name = "企业信息管理")
@RestController
@RequestMapping("/api/v1/ent-enterprise")
@RequiredArgsConstructor
public class EnterpriseController {

    private final EnterpriseService enterpriseService;

    @Operation(summary = "我的企业信息", description = "查看当前登录企业的完整信息(含法定代表人姓名/身份证号)")
    @GetMapping("/info")
    public Result<EnterpriseInfo> info() {
        return Result.success(enterpriseService.getMyEnterprise(LoginUserContext.require()));
    }

    @Operation(summary = "批量查询企业摘要", description = "按ID批量返回企业名称/信用代码(跨服务只读,如监管端引用企业档案;放行登录拦截)")
    @GetMapping("/batch")
    public Result<Map<Long, EnterpriseBriefVO>> batch(@RequestParam("ids") List<Long> ids) {
        return Result.success(enterpriseService.batchBrief(ids));
    }

    @Operation(summary = "修改法定代表人信息", description = "修改法定代表人姓名和身份证号(仅法人/管理员;审批端同步走此接口)")
    @PutMapping("/legal-person")
    public Result<Void> updateLegalPerson(@RequestBody LegalPersonUpdateReq req) {
        var user = LoginUserContext.require();
        if (!"ADMIN".equals(user.getRole())) {
            throw new BusinessException("仅法人/管理员可修改法定代表人信息,经办人请走变更登记");
        }
        enterpriseService.updateLegalPerson(user, req);
        return Result.success(null);
    }
}
