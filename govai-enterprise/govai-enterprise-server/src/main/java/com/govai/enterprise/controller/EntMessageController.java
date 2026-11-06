package com.govai.enterprise.controller;

import com.govai.enterprise.common.LoginUser;
import com.govai.enterprise.common.LoginUserContext;
import com.govai.enterprise.common.Result;
import com.govai.enterprise.entity.EntMessage;
import com.govai.enterprise.service.EntMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 企业站内消息接口(办结/证照/政策提醒 + 未读角标)
 */
@Tag(name = "企业站内消息")
@RestController
@RequestMapping("/api/v1/ent-message")
@RequiredArgsConstructor
public class EntMessageController {

    private final EntMessageService messageService;

    @Operation(summary = "消息列表")
    @GetMapping("/list")
    public Result<List<EntMessage>> list(@RequestParam(required = false) String type) {
        LoginUser user = LoginUserContext.require();
        return Result.success(messageService.listByEnterprise(user.getEnterpriseId(), type));
    }

    @Operation(summary = "未读数")
    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        LoginUser user = LoginUserContext.require();
        return Result.success(messageService.unreadCount(user.getEnterpriseId()));
    }

    @Operation(summary = "标记已读")
    @PostMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        LoginUser user = LoginUserContext.require();
        messageService.markRead(user.getEnterpriseId(), id);
        return Result.success();
    }

    @Operation(summary = "全部已读")
    @PostMapping("/read-all")
    public Result<Void> readAll() {
        LoginUser user = LoginUserContext.require();
        messageService.readAll(user.getEnterpriseId());
        return Result.success();
    }
}
