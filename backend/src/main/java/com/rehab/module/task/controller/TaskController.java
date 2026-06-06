package com.rehab.module.task.controller;

import com.rehab.common.PageResult;
import com.rehab.common.Result;
import com.rehab.common.exception.BusinessException;
import com.rehab.module.task.entity.Task;
import com.rehab.module.task.mapper.TaskMapper;
import com.rehab.module.task.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final TaskMapper taskMapper;

    /**
     * List tasks with filters and data-scope control.
     */
    @GetMapping
    public Result<PageResult<Task>> listTasks(
            @RequestParam(required = false) Long therapistId,
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResult<Task> result = taskService.listTasks(therapistId, groupId, patientId, status, date, page, size);
        return Result.ok(result);
    }

    /**
     * Get task detail by ID.
     */
    @GetMapping("/{id}")
    public Result<Task> getById(@PathVariable Long id) {
        return Result.ok(taskService.getById(id));
    }

    /**
     * Create a new task.
     */
    @PostMapping
    public Result<Task> createTask(@RequestBody Task task) {
        return Result.ok(taskService.createTask(task));
    }

    /**
     * Schedule a task — assign time slot and therapist.
     */
    @PutMapping("/{id}/schedule")
    public Result<Void> scheduleTask(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String timeSlot = (String) body.get("timeSlot");
        Long therapistId = body.get("therapistId") != null
                ? Long.valueOf(body.get("therapistId").toString())
                : null;
        taskService.scheduleTask(id, timeSlot, therapistId);
        return Result.ok();
    }

    /**
     * Start a task — mark it IN_PROGRESS.
     */
    @PostMapping("/{id}/start")
    public Result<Void> startTask(@PathVariable Long id) {
        taskService.startTask(id);
        return Result.ok();
    }

    /**
     * Verify (nuclear write-off) a task — complete verification with workload stats.
     */
    @PostMapping("/{id}/verify")
    public Result<Void> verifyTask(@PathVariable Long id) {
        taskService.verifyTask(id);
        return Result.ok();
    }

    /**
     * Revoke a task verification.
     */
    @PostMapping("/{id}/revoke")
    public Result<Void> revokeVerification(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String reason = body.getOrDefault("reason", "");
        taskService.revokeVerification(id, reason);
        return Result.ok();
    }

    /**
     * Get tasks for calendar view (by year/month).
     */
    @GetMapping("/calendar")
    public Result<List<Task>> getCalendarTasks(
            @RequestParam(required = false) Long therapistId,
            @RequestParam int year,
            @RequestParam int month) {
        return Result.ok(taskService.getCalendarTasks(therapistId, year, month));
    }

    @GetMapping("/treatment-items")
    public Result<List<String>> getTreatmentItems() {
        return Result.ok(taskService.getTreatmentItems());
    }

    @GetMapping("/check-conflict")
    public Result<Map<String, Object>> checkConflict(@RequestParam Long therapistId,
                                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate taskDate,
                                                      @RequestParam String timeSlot) {
        boolean hasConflict = taskMapper.existsConflict(therapistId, taskDate, timeSlot);
        Map<String, Object> result = new HashMap<>();
        result.put("conflict", hasConflict);
        if (hasConflict) {
            result.put("message", "该治疗师在 " + taskDate + " " + timeSlot + " 已有任务安排");
        }
        return Result.ok(result);
    }

    @PostMapping("/schedule-custom")
    public Result<Task> createScheduledTask(@RequestBody Map<String, Object> body) {
        Long patientId = Long.valueOf(body.get("patientId").toString());
        Long orderId = body.get("orderId") != null ? Long.valueOf(body.get("orderId").toString()) : null;
        Long therapistId = Long.valueOf(body.get("therapistId").toString());
        String taskDateStr = body.get("taskDate").toString();
        LocalDate taskDate = LocalDate.parse(taskDateStr);
        String timeSlot = body.get("timeSlot").toString();
        String treatmentItem = body.get("treatmentItem").toString();

        // Conflict check
        if (taskMapper.existsConflict(therapistId, taskDate, timeSlot)) {
            throw new BusinessException("日程冲突: 该治疗师在 " + taskDate + " " + timeSlot + " 已有任务安排");
        }

        Task task = new Task();
        task.setPatientId(patientId);
        task.setOrderId(orderId);
        task.setTherapistId(therapistId);
        task.setTaskDate(taskDate);
        task.setTimeSlot(timeSlot);
        task.setTreatmentItem(treatmentItem);
        task.setStatus("PENDING");

        return Result.ok(taskService.createTask(task));
    }
}
