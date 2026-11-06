package com.govai.enterprise.controller;

import com.govai.enterprise.common.LoginUserContext;
import com.govai.enterprise.common.Result;
import com.govai.enterprise.dto.EnterpriseMemberVO;
import com.govai.enterprise.dto.OperatorCreateReq;
import com.govai.enterprise.entity.EnterpriseUser;
import com.govai.enterprise.service.EnterpriseUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 企业用户(经办人)管理接口(前缀 ent-enterprise/users)
 * 仅法人/管理员(ADMIN)可调用;跨服务只读 members 除外
 */
@Tag(name = "经办人管理")
@RestController
@RequestMapping("/api/v1/ent-enterprise/users")
@RequiredArgsConstructor
public class EnterpriseUserController {

    private final EnterpriseUserService userService;

    @Operation(summary = "本企业用户列表(法人+经办人)", description = "仅法人/管理员可见")
    @GetMapping
    public Result<List<EnterpriseUser>> list() {
        return Result.success(userService.listByEnterprise(LoginUserContext.require()));
    }

    @Operation(summary = "按企业查法人+成员", description = "跨服务只读(监管端展示企业人员构成);放行登录拦截")
    @GetMapping("/members")
    public Result<EnterpriseMemberVO> members(@RequestParam Long enterpriseId) {
        return Result.success(userService.membersOf(enterpriseId));
    }

    @Operation(summary = "新增经办人", description = "法人添加本企业经办人,默认密码 123456")
    @PostMapping
    public Result<Void> add(@RequestBody OperatorCreateReq req) {
        userService.addOperator(LoginUserContext.require(), req);
        return Result.success(null);
    }

    @Operation(summary = "禁用经办人", description = "法人禁用本企业经办人(不能禁用自己或法人账号)")
    @PutMapping("/{id}/disable")
    public Result<Void> disable(@PathVariable Long id) {
        userService.disableOperator(LoginUserContext.require(), id);
        return Result.success(null);
    }

    // ===== 跨服只读/处置(监管端调用,放行登录拦截) =====

    @Operation(summary = "全部企业用户(含企业名)", description = "跨服只读,供监管端用户管理展示")
    @GetMapping("/all")
    public Result<List<EnterpriseUser>> all() {
        return Result.success(userService.listAllWithEnterpriseName());
    }

    @Operation(summary = "跨服禁用经办人", description = "监管端作为监管处置调用;不能禁用法人账号")
    @PostMapping("/members/{id}/disable")
    public Result<Void> disableCross(@PathVariable Long id) {
        userService.disableOperatorCross(id);
        return Result.success(null);
    }

    @Operation(summary = "跨服启用经办人", description = "监管端解除禁用,恢复其企业端登录")
    @PostMapping("/members/{id}/enable")
    public Result<Void> enableCross(@PathVariable Long id) {
        userService.enableOperatorCross(id);
        return Result.success(null);
    }
}
