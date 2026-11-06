package com.govai.citizen.controller;

import com.govai.citizen.common.PageResult;
import com.govai.citizen.common.Result;
import com.govai.citizen.dto.MatterDetailVO;
import com.govai.citizen.dto.MatterListVO;
import com.govai.citizen.dto.MatterQuery;
import com.govai.citizen.dto.MatterSaveReq;
import com.govai.citizen.entity.MatterCategory;
import com.govai.citizen.service.MatterService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/matters")
@RequiredArgsConstructor
public class MatterController {

    private final MatterService matterService;

    /**
     * 事项分类列表
     */
    @GetMapping("/categories")
    public Result<List<MatterCategory>> categories() {
        return Result.success(matterService.categories());
    }

    /**
     * 事项分页检索(关键字全文检索 + 分类筛选)
     */
    @GetMapping("/list")
    public Result<PageResult<MatterListVO>> list(MatterQuery query) {
        return Result.success(matterService.page(query));
    }

    /**
     * 事项详情(跨服务契约: 审批端拉取事项信息也走此接口, GET 公开)
     */
    @GetMapping("/{id}")
    public Result<MatterDetailVO> detail(@PathVariable("id") Long id) {
        return Result.success(matterService.detail(id));
    }

    @PostMapping
    public Result<Long> create(@RequestBody MatterSaveReq req) {
        return Result.success(matterService.save(req));
    }

    @PutMapping("/{id}")
    public Result<Long> update(@PathVariable("id") Long id, @RequestBody MatterSaveReq req) {
        req.setId(id);
        return Result.success(matterService.save(req));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        matterService.delete(id);
        return Result.success();
    }
}
