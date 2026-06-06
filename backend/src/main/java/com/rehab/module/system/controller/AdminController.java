package com.rehab.module.system.controller;

import com.rehab.common.PageResult;
import com.rehab.common.Result;
import com.rehab.module.auth.entity.User;
import com.rehab.module.system.entity.OperationLog;
import com.rehab.module.system.entity.SystemDict;
import com.rehab.module.system.entity.TherapyGroup;
import com.rehab.module.system.service.SystemService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final SystemService systemService;

    // ================================================================
    // User management
    // ================================================================

    @GetMapping("/users")
    public Result<PageResult<User>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role) {
        return Result.ok(systemService.listUsers(page, size, keyword, role));
    }

    @PostMapping("/users")
    public Result<User> createUser(@RequestBody User user) {
        return Result.ok(systemService.createUser(user));
    }

    @PutMapping("/users/{id}")
    public Result<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        user.setId(id);
        return Result.ok(systemService.updateUser(user));
    }

    @DeleteMapping("/users/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        systemService.deleteUser(id);
        return Result.ok();
    }

    @PutMapping("/users/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id) {
        systemService.resetPassword(id);
        return Result.ok();
    }

    // ================================================================
    // Therapy group management
    // ================================================================

    @GetMapping("/therapy-groups")
    public Result<List<TherapyGroup>> listGroups() {
        return Result.ok(systemService.listGroups());
    }

    @PostMapping("/therapy-groups")
    public Result<TherapyGroup> createGroup(@RequestBody TherapyGroup group) {
        return Result.ok(systemService.createGroup(group));
    }

    @PutMapping("/therapy-groups/{id}")
    public Result<TherapyGroup> updateGroup(@PathVariable Long id, @RequestBody TherapyGroup group) {
        group.setId(id);
        return Result.ok(systemService.updateGroup(group));
    }

    @DeleteMapping("/therapy-groups/{id}")
    public Result<Void> deleteGroup(@PathVariable Long id) {
        systemService.deleteGroup(id);
        return Result.ok();
    }

    // ================================================================
    // Dict management
    // ================================================================

    @GetMapping("/dicts")
    public Result<List<SystemDict>> listDicts(@RequestParam String type) {
        return Result.ok(systemService.listByType(type));
    }

    @GetMapping("/dicts/types")
    public Result<List<String>> listAllTypes() {
        return Result.ok(systemService.listAllTypes());
    }

    @PostMapping("/dicts")
    public Result<SystemDict> createDict(@RequestBody SystemDict dict) {
        return Result.ok(systemService.createDict(dict));
    }

    @PutMapping("/dicts/{id}")
    public Result<SystemDict> updateDict(@PathVariable Long id, @RequestBody SystemDict dict) {
        dict.setId(id);
        return Result.ok(systemService.updateDict(dict));
    }

    @DeleteMapping("/dicts/{id}")
    public Result<Void> deleteDict(@PathVariable Long id) {
        systemService.deleteDict(id);
        return Result.ok();
    }

    // ================================================================
    // Operation logs
    // ================================================================

    @GetMapping("/logs")
    public Result<PageResult<OperationLog>> listLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return Result.ok(systemService.listLogs(page, size, userId, action, startDate, endDate));
    }

    @GetMapping("/logs/{id}")
    public Result<OperationLog> getLogById(@PathVariable Long id) {
        return Result.ok(systemService.getLogById(id));
    }

    // ================================================================
    // System overview stats
    // ================================================================

    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        return Result.ok(systemService.getSystemStats());
    }

    @GetMapping("/stats/workload-by-role")
    public Result<List<Map<String, Object>>> getWorkloadByRole(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        return Result.ok(systemService.getWorkloadByRole(startDate, endDate));
    }

    @GetMapping("/stats/treatment-distribution")
    public Result<List<Map<String, Object>>> getTreatmentDistribution(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        return Result.ok(systemService.getTreatmentTypeDistribution(startDate, endDate));
    }

    @GetMapping("/stats/daily-trend")
    public Result<List<Map<String, Object>>> getDailyTrend(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        return Result.ok(systemService.getDailyVerificationTrend(startDate, endDate));
    }
}
