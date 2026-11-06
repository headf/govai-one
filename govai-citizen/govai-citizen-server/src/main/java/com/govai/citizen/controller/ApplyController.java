package com.govai.citizen.controller;

import com.govai.citizen.common.PageResult;
import com.govai.citizen.common.Result;
import com.govai.citizen.dto.ApplyResultVO;
import com.govai.citizen.dto.ApplySubmitReq;
import com.govai.citizen.dto.RecordDetailVO;
import com.govai.citizen.dto.RecordListVO;
import com.govai.citizen.dto.RecordQuery;
import com.govai.citizen.service.ApplyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ApplyController {

    private final ApplyService applyService;

    /**
     * 在线申报(提交后自动落站内信/进度通知, 并调用审批端Feign提交审批)
     */
    @PostMapping("/apply")
    public Result<ApplyResultVO> submit(@Valid @RequestBody ApplySubmitReq req) {
        return Result.success(applyService.submit(req));
    }

    /**
     * 我的申报列表
     */
    @GetMapping("/records")
    public Result<PageResult<RecordListVO>> myRecords(RecordQuery query) {
        return Result.success(applyService.myRecords(query));
    }

    /**
     * 申报详情
     */
    @GetMapping("/records/{id}")
    public Result<RecordDetailVO> detail(@PathVariable("id") Long id) {
        return Result.success(applyService.detail(id));
    }

    /**
     * 开发期模拟审批通过(仅演示用, 审批端联调后由真实审批推送办结)
     */
    @PostMapping("/records/{id}/mock-approve")
    public Result<Void> mockApprove(@PathVariable("id") Long id) {
        applyService.mockApprove(id);
        return Result.success();
    }

    /**
     * 撤回申报(仅本地待受理、未进入审批流转的办件)
     */
    @PostMapping("/records/{id}/withdraw")
    public Result<Void> withdraw(@PathVariable("id") Long id) {
        applyService.withdraw(id);
        return Result.success();
    }
}
