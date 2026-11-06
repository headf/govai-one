package com.govai.citizen.controller;

import com.govai.citizen.common.PageResult;
import com.govai.citizen.common.Result;
import com.govai.citizen.dto.MessageVO;
import com.govai.citizen.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping("/list")
    public Result<PageResult<MessageVO>> list(@RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int size,
                                              @RequestParam(required = false) String type) {
        return Result.success(messageService.page(page, size, type));
    }

    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        return Result.success(messageService.unreadCount());
    }

    @GetMapping("/{id}")
    public Result<MessageVO> detail(@PathVariable("id") Long id) {
        return Result.success(messageService.detail(id));
    }

    @PostMapping("/read-all")
    public Result<Void> readAll() {
        messageService.readAll();
        return Result.success();
    }
}
