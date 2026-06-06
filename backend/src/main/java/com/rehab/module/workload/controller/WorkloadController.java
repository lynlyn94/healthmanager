package com.rehab.module.workload.controller;

import com.alibaba.excel.EasyExcel;
import com.rehab.common.Result;
import com.rehab.infrastructure.UserContext;
import com.rehab.module.workload.service.WorkloadService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/workload")
@RequiredArgsConstructor
public class WorkloadController {

    private final WorkloadService workloadService;

    @GetMapping("/personal")
    public Result<Map<String, Object>> getPersonalStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.ok(workloadService.getPersonalStats(UserContext.getUserId(), startDate, endDate));
    }

    @GetMapping("/group")
    public Result<Map<String, Object>> getGroupStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.ok(workloadService.getGroupStats(UserContext.getGroupId(), startDate, endDate));
    }

    @GetMapping("/department")
    public Result<Map<String, Object>> getDepartmentStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.ok(workloadService.getDepartmentStats(startDate, endDate));
    }

    @GetMapping("/trend")
    public Result<List<Map<String, Object>>> getTrendData(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.ok(workloadService.getTrendData(UserContext.getUserId(), startDate, endDate));
    }

    @GetMapping("/export")
    public void exportStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String filename = URLEncoder.encode("工作量统计.xlsx", "UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=" + filename);

        List<Map<String, Object>> dataList = workloadService.exportStats(null, startDate, endDate);
        EasyExcel.write(response.getOutputStream(), Map.class).head(head()).sheet("工作量统计").doWrite(dataList);
    }

    private List<List<String>> head() {
        return Arrays.asList(
                Arrays.asList("日期"),
                Arrays.asList("治疗师"),
                Arrays.asList("治疗次数"),
                Arrays.asList("患者数"),
                Arrays.asList("治疗类型"));
    }
}
