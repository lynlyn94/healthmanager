package com.rehab.module.system.controller;

import com.rehab.common.PageResult;
import com.rehab.common.Result;
import com.rehab.infrastructure.UserContext;
import com.rehab.module.system.service.SystemService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final SystemService systemService;

    @GetMapping
    public Result<PageResult> listNotifications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = UserContext.getUserId();
        return Result.ok(systemService.listNotifications(userId, page, size));
    }

    @PutMapping("/{id}/read")
    public Result<Void> markAsRead(@PathVariable Long id) {
        systemService.markAsRead(id);
        return Result.ok();
    }

    @PutMapping("/read-all")
    public Result<Void> markAllAsRead() {
        Long userId = UserContext.getUserId();
        systemService.markAllAsRead(userId);
        return Result.ok();
    }

    @GetMapping("/unread-count")
    public Result<Long> getUnreadCount() {
        Long userId = UserContext.getUserId();
        return Result.ok(systemService.getUnreadCount(userId));
    }
}
