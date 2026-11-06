package com.govai.supervision.controller;

import com.govai.supervision.common.PageResult;
import com.govai.supervision.common.Result;
import com.govai.supervision.annotation.RequirePermission;
import com.govai.supervision.entity.SysDict;
import com.govai.supervision.service.SysDictService;
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
import java.util.Map;

/**
 * 数据字典控制器
 * /api/v1/dicts
 */
@Tag(name = "数据字典", description = "字典分组 / 条目 CRUD")
@RestController
@RequestMapping("/api/v1/dicts")
@RequiredArgsConstructor
public class SysDictController {

    private final SysDictService sysDictService;

    /** 分组查询（含所有条目，供左侧类型导航 + 右侧编辑） */
    @Operation(summary = "按类型分组返回全部字典")
    @GetMapping("/group")
    public Result<List<Map<String, Object>>> group() {
        return Result.success(sysDictService.groupByType());
    }

    /** 分页查询 */
    @Operation(summary = "字典分页")
    @GetMapping("/list")
    public Result<PageResult<SysDict>> list(@RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "10") long size,
                                            @RequestParam(required = false) String dictType,
                                            @RequestParam(required = false) String keyword) {
        return Result.success(sysDictService.pageDicts(page, size, dictType, keyword));
    }

    /** 按类型查启用条目（下拉用） */
    @Operation(summary = "按类型查启用条目")
    @GetMapping("/type/{dictType}")
    public Result<List<SysDict>> byType(@PathVariable String dictType) {
        return Result.success(sysDictService.listByType(dictType));
    }

    @Operation(summary = "新增字典")
    @RequirePermission("dict:manage")
    @PostMapping
    public Result<SysDict> create(@RequestBody SysDict dict) {
        return Result.success("新增成功", sysDictService.create(dict));
    }

    @Operation(summary = "更新字典")
    @RequirePermission("dict:manage")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody SysDict dict) {
        dict.setId(id);
        sysDictService.update(dict);
        return Result.success();
    }

    @Operation(summary = "删除字典")
    @RequirePermission("dict:manage")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sysDictService.delete(id);
        return Result.success();
    }
}
