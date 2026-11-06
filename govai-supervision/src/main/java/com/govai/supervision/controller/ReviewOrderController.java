package com.govai.supervision.controller;

import com.govai.supervision.common.PageResult;
import com.govai.supervision.common.Result;
import com.govai.supervision.entity.ReviewOrder;
import com.govai.supervision.interceptor.JwtInterceptor;
import com.govai.supervision.service.ReviewOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 差评整改工单控制器（好差评闭环）
 *
 * 数据来源：群众端 / 企业端提交好差评时，星级 <= 2 的差评由两端通过 Feign 上报到 /report，
 * 在本端生成一条待处置工单；监管员在"差评整改"页面处置，形成"差评 → 整改 → 反馈"闭环。
 *
 * 说明：/report 是跨端内部接口，已在 WebMvcConfig 白名单中放行（身份由上报方自证），
 * 与 /api/v1/cit-view/**、/api/v1/ent-view/** 同类；其余接口走监管端 JWT。
 */
@Tag(name = "差评整改工单", description = "差评上报 / 工单列表 / 整改处置")
@RestController
@RequestMapping("/api/v1/review-orders")
@RequiredArgsConstructor
public class ReviewOrderController {

    private final ReviewOrderService reviewOrderService;

    /** 差评上报（群众端 / 企业端 Feign 调用） */
    @Operation(summary = "差评上报（跨端内部接口）")
    @PostMapping("/report")
    public Result<Void> report(@RequestBody ReviewOrder order) {
        reviewOrderService.report(order);
        return Result.success();
    }

    /** 工单分页列表 */
    @Operation(summary = "差评工单分页列表")
    @GetMapping("/list")
    public Result<PageResult<ReviewOrder>> list(@RequestParam(defaultValue = "1") long page,
                                                @RequestParam(defaultValue = "10") long size,
                                                @RequestParam(required = false) String handleStatus,
                                                @RequestParam(required = false) String source) {
        return Result.success(reviewOrderService.page(page, size, handleStatus, source));
    }

    /** 待处置数量（工作台/大屏展示用） */
    @Operation(summary = "待处置工单数")
    @GetMapping("/pending-count")
    public Result<Long> pendingCount() {
        return Result.success(reviewOrderService.countPending());
    }

    /** 处置工单（填写整改意见） */
    @Operation(summary = "处置差评工单")
    @PostMapping("/{id}/handle")
    public Result<Void> handle(@PathVariable Long id,
                               @RequestParam(required = false) String opinion,
                               HttpServletRequest request) {
        Object nameAttr = request.getAttribute(JwtInterceptor.ATTR_USER_NAME);
        String handlerName = nameAttr == null ? null : nameAttr.toString();
        reviewOrderService.handle(id, opinion, handlerName);
        return Result.success();
    }
}
