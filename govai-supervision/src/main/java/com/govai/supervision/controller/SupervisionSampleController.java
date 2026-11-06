package com.govai.supervision.controller;

import com.govai.supervision.common.PageResult;
import com.govai.supervision.common.Result;
import com.govai.supervision.annotation.RequirePermission;
import com.govai.supervision.dto.sample.TargetedSampleReq;
import com.govai.supervision.entity.SupervisionSample;
import com.govai.supervision.interceptor.JwtInterceptor;
import com.govai.supervision.service.SupervisionSampleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 抽样检查控制器
 * 契约命名：/api/v1/samples
 */
@Tag(name = "抽样检查", description = "随机抽样 / 定向抽查 / 记录查询")
@RestController
@RequestMapping("/api/v1/samples")
@RequiredArgsConstructor
public class SupervisionSampleController {

    private final SupervisionSampleService supervisionSampleService;

    /** 抽样记录分页 */
    @Operation(summary = "抽样记录分页")
    @GetMapping("/list")
    public Result<PageResult<SupervisionSample>> list(@RequestParam(defaultValue = "1") long page,
                                                      @RequestParam(defaultValue = "10") long size,
                                                      @RequestParam(required = false) String sampleType,
                                                      @RequestParam(required = false) String checkResult) {
        return Result.success(supervisionSampleService.pageSamples(page, size, sampleType, checkResult));
    }

    /** 随机抽样 */
    @Operation(summary = "随机抽样", description = "creditWeighted=true 时按申报企业信用等级差异化抽取(D级最优先)")
    @RequirePermission("sample:operate")
    @PostMapping("/random")
    public Result<Void> random(@RequestParam(defaultValue = "5") int count,
                               @RequestParam(defaultValue = "false") boolean creditWeighted,
                               HttpServletRequest request) {
        Long checkerId = (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID);
        supervisionSampleService.randomSample(count, checkerId, creditWeighted);
        return Result.success();
    }

    /** 定向抽查 */
    @Operation(summary = "定向抽查")
    @RequirePermission("sample:operate")
    @PostMapping("/targeted")
    public Result<SupervisionSample> targeted(@RequestBody @Valid TargetedSampleReq req, HttpServletRequest request) {
        Long checkerId = (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID);
        return Result.success("定向检查完成", supervisionSampleService.targetedSample(
                req.getInstanceId(), req.getCheckResult(), req.getCheckDetail(), checkerId));
    }
}