package com.rehab.module.schedule.controller;

import com.rehab.common.Result;
import com.rehab.infrastructure.UserContext;
import com.rehab.module.schedule.entity.PatientSchedule;
import com.rehab.module.schedule.service.ScheduleService;
import com.rehab.module.task.entity.Task;
import com.rehab.module.task.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final TaskService taskService;

    @GetMapping("/api/v1/patients/{id}/schedule")
    public Result<List<PatientSchedule>> getByPatient(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.ok(scheduleService.getByPatient(id, startDate, endDate));
    }

    @GetMapping("/api/v1/schedule/my")
    public Result<List<PatientSchedule>> getMySchedule(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.ok(scheduleService.getMySchedule(startDate, endDate));
    }

    @GetMapping("/api/v1/schedule/group")
    public Result<List<PatientSchedule>> getGroupSchedule(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.ok(scheduleService.getGroupSchedule(UserContext.getGroupId(), startDate, endDate));
    }

    @PostMapping("/api/v1/schedule/{id}/generate-task")
    public Result<Task> generateTask(@PathVariable Long id) {
        return Result.ok(taskService.generateTaskFromSchedule(id));
    }
}
