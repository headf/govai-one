package com.govai.enterprise.controller;

import com.govai.enterprise.common.LoginUserContext;
import com.govai.enterprise.common.PageResult;
import com.govai.enterprise.common.Result;
import com.govai.enterprise.dto.MatterPlanReq;
import com.govai.enterprise.dto.MatterPlanResp;
import com.govai.enterprise.dto.MatterQuery;
import com.govai.enterprise.dto.MatterVO;
import com.govai.enterprise.entity.EntMatterCategory;
import com.govai.enterprise.service.MatterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "法人事项大厅")
@RestController
@RequestMapping("/api/v1/ent-matters")
@RequiredArgsConstructor
public class MatterController {

    private final MatterService matterService;

    @Operation(summary = "事项分页检索")
    @GetMapping("/list")
    public Result<PageResult<MatterVO>> list(MatterQuery query) {
        return Result.success(matterService.pageQuery(query));
    }

    @Operation(summary = "按事项编码查详情", description = "供前端按编码动态解析事项(兑现/延续/变更跳转),避免硬编码 id")
    @GetMapping("/by-code")
    public Result<MatterVO> detailByCode(@RequestParam String code) {
        return Result.success(matterService.detailByCode(code));
    }

    @Operation(summary = "事项详情")
    @GetMapping("/{id}")
    public Result<MatterVO> detail(@PathVariable Long id) {
        return Result.success(matterService.detail(id));
    }

    @Operation(summary = "事项分类列表")
    @GetMapping("/categories")
    public Result<List<EntMatterCategory>> categories() {
        return Result.success(matterService.categories());
    }

    @Operation(summary = "智能办事规划", description = "输入业务需求描述,自动生成所需办理事项清单及办理顺序建议,并保存为历史")
    @PostMapping("/plan")
    public Result<MatterPlanResp> plan(@RequestBody(required = false) MatterPlanReq req) {
        if (req == null) {
            req = new MatterPlanReq();
        }
        return Result.success(matterService.planMatters(LoginUserContext.require(), req));
    }

    @Operation(summary = "我的规划历史", description = "查询当前企业的智能办事规划历史")
    @GetMapping("/plan/history")
    public Result<List<com.govai.enterprise.entity.EntMatterPlanHistory>> planHistory() {
        return Result.success(matterService.listPlanHistory(LoginUserContext.require()));
    }

    @Operation(summary = "规划历史详情", description = "恢复某条历史规划的完整内容")
    @GetMapping("/plan/history/{id}")
    public Result<MatterPlanResp> planHistoryDetail(@PathVariable Long id) {
        return Result.success(matterService.getPlanHistory(LoginUserContext.require(), id));
    }

    @Operation(summary = "确认完成规划", description = "所有事项办结后确认完成本规划")
    @PutMapping("/plan/history/{id}/complete")
    public Result<Void> completePlan(@PathVariable Long id) {
        matterService.completePlan(LoginUserContext.require(), id);
        return Result.success(null);
    }

    @Operation(summary = "删除规划历史")
    @DeleteMapping("/plan/history/{id}")
    public Result<Void> deletePlan(@PathVariable Long id) {
        matterService.deletePlan(LoginUserContext.require(), id);
        return Result.success(null);
    }
}
