package com.govai.supervision.controller;

import com.govai.supervision.common.PageResult;
import com.govai.supervision.common.Result;
import com.govai.supervision.annotation.RequirePermission;
import com.govai.supervision.entity.SysLog;
import com.govai.supervision.service.SysLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统操作日志控制器
 * /api/v1/logs
 */
@Tag(name = "操作日志", description = "查询 / 清空")
@RestController
@RequestMapping("/api/v1/logs")
@RequiredArgsConstructor
public class SysLogController {

    private final SysLogService sysLogService;

    @Operation(summary = "日志分页查询")
    @GetMapping("/list")
    public Result<PageResult<SysLog>> list(@RequestParam(defaultValue = "1") long page,
                                           @RequestParam(defaultValue = "10") long size,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) String begin,
                                           @RequestParam(required = false) String end) {
        return Result.success(sysLogService.pageLogs(page, size, keyword, begin, end));
    }

    @Operation(summary = "清空日志（admin）")
    @RequirePermission("log:clear")
    @DeleteMapping
    public Result<Void> clear() {
        sysLogService.clearAll();
        return Result.<Void>success("日志已清空", null);
    }
}
