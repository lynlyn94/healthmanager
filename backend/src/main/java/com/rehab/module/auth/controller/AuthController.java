package com.rehab.module.auth.controller;

import com.rehab.common.Result;
import com.rehab.infrastructure.UserContext;
import com.rehab.module.auth.dto.LoginRequest;
import com.rehab.module.auth.dto.LoginResponse;
import com.rehab.module.auth.entity.User;
import com.rehab.module.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    @GetMapping("/userinfo")
    public Result<LoginResponse> userinfo() {
        User user = authService.getUserInfo(UserContext.getUserId());
        return Result.ok(LoginResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .role(user.getRole())
                .groupId(user.getGroupId())
                .phone(user.getPhone())
                .email(user.getEmail())
                .avatar(user.getAvatar())
                .build());
    }

    @PutMapping("/userinfo")
    public Result<Void> updateUserInfo(@RequestBody Map<String, String> params) {
        authService.updateUserInfo(UserContext.getUserId(),
                params.get("realName"), params.get("phone"), params.get("email"), params.get("avatar"));
        return Result.ok();
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody Map<String, String> params) {
        authService.changePassword(UserContext.getUserId(),
                params.get("oldPassword"), params.get("newPassword"));
        return Result.ok();
    }
}
