package com.govai.citizen.controller;

import com.govai.citizen.common.Result;
import com.govai.citizen.dto.UpdateProfileReq;
import com.govai.citizen.dto.UserProfileVO;
import com.govai.citizen.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public Result<UserProfileVO> profile() {
        return Result.success(userService.profile());
    }

    @PutMapping("/profile")
    public Result<UserProfileVO> update(@RequestBody UpdateProfileReq req) {
        return Result.success(userService.update(req));
    }
}
