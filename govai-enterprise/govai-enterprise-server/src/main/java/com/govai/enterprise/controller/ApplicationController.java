package com.govai.enterprise.controller;

import com.govai.enterprise.common.LoginUser;
import com.govai.enterprise.common.LoginUserContext;
import com.govai.enterprise.common.PageResult;
import com.govai.enterprise.common.Result;
import com.govai.enterprise.dto.ApplyDetailVO;
import com.govai.enterprise.dto.ApplyRecordVO;
import com.govai.enterprise.dto.ApplySubmitReq;
import com.govai.enterprise.dto.ApplySubmitResp;
import com.govai.enterprise.dto.PreCheckResp;
import com.govai.enterprise.dto.ReuseMaterialReq;
import com.govai.enterprise.dto.ReusableMaterialVO;
import com.govai.enterprise.entity.EntApplicationMaterial;
import com.govai.enterprise.entity.EnterpriseLicense;
import com.govai.enterprise.service.ApplicationService;
import com.govai.enterprise.service.ComplianceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 企业申报接口(需登录;路径前缀 ent-apply)
 */
@Tag(name = "企业申报")
@RestController
@RequestMapping("/api/v1/ent-apply")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;
    private final ComplianceService complianceService;

    @Operation(summary = "创建申报草稿", description = "选择事项+填表后创建记录,返回 recordId 供后续上传材料")
    @PostMapping("/draft")
    public Result<ApplySubmitResp> draft(@Valid @RequestBody ApplySubmitReq req) {
        return Result.success(applicationService.createDraft(LoginUserContext.require(), req));
    }

    @Operation(summary = "最终提交申报", description = "校验材料后调审批端,返回审批实例ID")
    @PostMapping("/{recordId}/submit")
    public Result<ApplySubmitResp> submit(@PathVariable Long recordId) {
        return Result.success(applicationService.submit(LoginUserContext.require(), recordId));
    }

    @Operation(summary = "合规预检", description = "提交前自动检查表单/证照/材料一致性,返回预检报告")
    @PostMapping("/{recordId}/precheck")
    public Result<PreCheckResp> preCheck(@PathVariable Long recordId) {
        LoginUser user = LoginUserContext.require();
        return Result.success(complianceService.preCheck(user.getEnterpriseId(), recordId));
    }

    @Operation(summary = "上传申报材料")
    @PostMapping("/{recordId}/material")
    public Result<EntApplicationMaterial> uploadMaterial(@PathVariable Long recordId,
                                                         @RequestParam("materialName") String materialName,
                                                         @RequestParam("file") MultipartFile file) {
        return Result.success(applicationService.uploadMaterial(
                LoginUserContext.require(), recordId, materialName, file));
    }

    @Operation(summary = "可复用的证照与历史材料", description = "供材料上传环节从已有证照/历史材料中选择,避免重复上传")
    @GetMapping("/reusable-materials")
    public Result<ReusableMaterialVO> reusableMaterials() {
        return Result.success(applicationService.reusableMaterials(LoginUserContext.require()));
    }

    @Operation(summary = "复用已有材料", description = "从电子证照或历史材料中选择,直接关联已有文件")
    @PostMapping("/{recordId}/material/reuse")
    public Result<EntApplicationMaterial> reuseMaterial(@PathVariable Long recordId,
                                                       @Valid @RequestBody ReuseMaterialReq req) {
        return Result.success(applicationService.reuseMaterial(LoginUserContext.require(), recordId, req));
    }

    @Operation(summary = "免证办:匹配可自动带证的电子证照", description = "按事项材料清单匹配企业名下有效证照")
    @GetMapping("/licenses/match")
    public Result<List<EnterpriseLicense>> matchLicenses(@RequestParam Long matterId) {
        return Result.success(applicationService.matchLicenses(LoginUserContext.require(), matterId));
    }

    @Operation(summary = "表单预填:企业信息库基础字段", description = "返回当前企业信息(企业名称/信用代码/法人/地址/电话),供申报表单自动回填,已有营业执照的企业无需重复填表或拍照")
    @GetMapping("/prefill")
    public Result<Map<String, String>> prefill() {
        return Result.success(applicationService.prefillEnterpriseInfo(LoginUserContext.require()));
    }

    @Operation(summary = "我的申报列表")
    @GetMapping("/list")
    public Result<PageResult<ApplyRecordVO>> list(@RequestParam(defaultValue = "1") Integer page,
                                                  @RequestParam(defaultValue = "10") Integer size,
                                                  @RequestParam(required = false) String keyword) {
        return Result.success(applicationService.pageMyRecords(LoginUserContext.require(), page, size, keyword));
    }

    @Operation(summary = "草稿箱列表")
    @GetMapping("/drafts")
    public Result<PageResult<ApplyRecordVO>> drafts(@RequestParam(defaultValue = "1") Integer page,
                                                    @RequestParam(defaultValue = "10") Integer size,
                                                    @RequestParam(required = false) String keyword) {
        return Result.success(applicationService.pageMyDrafts(LoginUserContext.require(), page, size, keyword));
    }

    @Operation(summary = "删除草稿")
    @DeleteMapping("/draft/{recordId}")
    public Result<Void> deleteDraft(@PathVariable Long recordId) {
        applicationService.deleteDraft(LoginUserContext.require(), recordId);
        return Result.success();
    }

    @Operation(summary = "申报详情(含材料/预检/进度)")
    @GetMapping("/{recordId}")
    public Result<ApplyDetailVO> detail(@PathVariable Long recordId) {
        return Result.success(applicationService.detail(LoginUserContext.require(), recordId));
    }

    @Operation(summary = "审批结果回调(审批端推送,企业端办结出证)")
    @PostMapping("/{recordId}/approval-result")
    public Result<String> approvalResult(@PathVariable Long recordId,
                                         @RequestParam String status,
                                         @RequestParam(required = false) String remark) {
        String issuedNo = applicationService.handleApprovalResult(recordId, status, remark);
        return Result.success(issuedNo);
    }

    @Operation(summary = "审批结果回调-按审批实例(审批端定稿推送,服务间免登录)")
    @PostMapping("/approval-result-by-instance")
    public Result<String> approvalResultByInstance(@RequestParam Long instanceId,
                                                   @RequestParam String status,
                                                   @RequestParam(required = false) String remark) {
        String issuedNo = applicationService.handleApprovalResultByInstance(instanceId, status, remark);
        return Result.success(issuedNo);
    }

    @Operation(summary = "按审批实例查申报材料(审批端复核材料用,服务间免登录)")
    @GetMapping("/materials-by-instance/{instanceId}")
    public Result<java.util.List<com.govai.enterprise.entity.EntApplicationMaterial>> materialsByInstance(
            @PathVariable Long instanceId) {
        return Result.success(applicationService.materialsByInstance(instanceId));
    }
}
