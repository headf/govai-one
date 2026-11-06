package com.govai.enterprise.controller;

import com.govai.enterprise.common.LoginUserContext;
import com.govai.enterprise.common.PageResult;
import com.govai.enterprise.common.Result;
import com.govai.enterprise.dto.QaAskReq;
import com.govai.enterprise.dto.QaAskResp;
import com.govai.enterprise.dto.QaHistoryVO;
import com.govai.enterprise.dto.QaSessionVO;
import com.govai.enterprise.service.QaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 智能问答接口(支持会话维度历史切换)
 */
@Tag(name = "智能问答")
@RestController
@RequestMapping("/api/v1/ent-qa")
@RequiredArgsConstructor
public class QaController {

    private final QaService qaService;

    @Operation(summary = "智能问答", description = "提问并返回答案;带 sessionId 则归入该会话,否则后端自动新建会话")
    @PostMapping("/ask")
    public Result<QaAskResp> ask(@Valid @RequestBody QaAskReq req) {
        return Result.success(qaService.ask(LoginUserContext.require(), req));
    }

    @Operation(summary = "推荐问题")
    @GetMapping("/suggestions")
    public Result<List<String>> suggestions() {
        return Result.success(qaService.suggestions());
    }

    @Operation(summary = "新建会话", description = "创建一个空会话,返回会话ID供后续提问关联")
    @PostMapping("/session")
    public Result<QaSessionVO> createSession() {
        return Result.success(qaService.createSession(LoginUserContext.require()));
    }

    @Operation(summary = "会话列表", description = "按最后活跃时间倒序分页,返回当前企业的会话列表")
    @GetMapping("/sessions")
    public Result<PageResult<QaSessionVO>> sessions(@RequestParam(defaultValue = "1") Integer page,
                                                     @RequestParam(defaultValue = "20") Integer size) {
        return Result.success(qaService.listSessions(LoginUserContext.require(), page, size));
    }

    @Operation(summary = "会话消息列表", description = "加载指定会话的全部问答消息(按时间升序)")
    @GetMapping("/session/{sessionId}")
    public Result<List<QaHistoryVO>> sessionMessages(@PathVariable Long sessionId) {
        return Result.success(qaService.getSessionMessages(LoginUserContext.require(), sessionId));
    }

    @Operation(summary = "删除会话", description = "删除指定会话及其全部问答记录")
    @DeleteMapping("/session/{sessionId}")
    public Result<Void> deleteSession(@PathVariable Long sessionId) {
        qaService.deleteSession(LoginUserContext.require(), sessionId);
        return Result.success();
    }

    @Operation(summary = "问答历史(会话列表)", description = "兼容旧路径:返回会话列表(非单条Q&A)")
    @GetMapping("/history")
    public Result<PageResult<QaSessionVO>> history(@RequestParam(defaultValue = "1") Integer page,
                                                   @RequestParam(defaultValue = "20") Integer size) {
        return Result.success(qaService.history(LoginUserContext.require(), page, size));
    }

    @Operation(summary = "清空问答历史", description = "清空当前企业全部会话与问答记录")
    @DeleteMapping("/history")
    public Result<Void> clearHistory() {
        qaService.clearHistory(LoginUserContext.require());
        return Result.success();
    }
}
