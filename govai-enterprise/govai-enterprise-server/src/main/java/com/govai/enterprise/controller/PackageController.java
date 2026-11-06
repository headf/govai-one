package com.govai.enterprise.controller;

import com.govai.enterprise.common.LoginUser;
import com.govai.enterprise.common.LoginUserContext;
import com.govai.enterprise.common.Result;
import com.govai.enterprise.dto.PackageSubmitReq;
import com.govai.enterprise.dto.PackageSubmitResp;
import com.govai.enterprise.dto.PackageVO;
import com.govai.enterprise.service.PackageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 一件事套餐接口
 */
@Tag(name = "一件事套餐")
@RestController
@RequestMapping("/api/v1/ent-package")
@RequiredArgsConstructor
public class PackageController {

    private final PackageService packageService;

    @Operation(summary = "套餐列表")
    @GetMapping("/list")
    public Result<List<PackageVO>> list() {
        return Result.success(packageService.listPackages());
    }

    @Operation(summary = "套餐详情(含合并表单/材料)")
    @GetMapping("/{id}")
    public Result<PackageVO> detail(@PathVariable Long id) {
        return Result.success(packageService.detail(id));
    }

    @Operation(summary = "套餐材料上传", description = "一表申报合并清单中的材料:先存文件返回URL,提交时再关联到拆分出的各子申报")
    @PostMapping("/upload")
    public Result<java.util.Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        return Result.success(packageService.uploadMaterial(file));
    }

    @Operation(summary = "一表申报提交(拆分多事项)")
    @PostMapping("/{id}/submit")
    public Result<PackageSubmitResp> submit(@PathVariable Long id, @Valid @RequestBody PackageSubmitReq req) {
        req.setPackageId(id);
        return Result.success(packageService.submit(LoginUserContext.require(), req));
    }
}
