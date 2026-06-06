package com.rehab.module.workload.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rehab.module.auth.entity.User;
import com.rehab.module.auth.mapper.UserMapper;
import com.rehab.module.workload.entity.WorkloadStat;
import com.rehab.module.workload.mapper.WorkloadStatMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkloadService {

    private final WorkloadStatMapper workloadStatMapper;
    private final UserMapper userMapper;

    /**
     * Aggregate stats for a single therapist.
     */
    public Map<String, Object> getPersonalStats(Long userId, LocalDate startDate, LocalDate endDate) {
        List<WorkloadStat> stats = workloadStatMapper.selectList(
                new LambdaQueryWrapper<WorkloadStat>()
                        .eq(WorkloadStat::getUserId, userId)
                        .between(WorkloadStat::getStatDate, startDate, endDate)
        );
        return aggregatePersonalStats(stats, userId);
    }

    /**
     * Aggregate stats for all users in a group.
     */
    public Map<String, Object> getGroupStats(Long groupId, LocalDate startDate, LocalDate endDate) {
        List<Long> userIds = userMapper.selectList(
                new LambdaQueryWrapper<User>().eq(User::getGroupId, groupId)
        ).stream().map(User::getId).collect(Collectors.toList());

        if (userIds.isEmpty()) {
            return emptyStats(groupId);
        }

        List<WorkloadStat> stats = workloadStatMapper.selectList(
                new LambdaQueryWrapper<WorkloadStat>()
                        .in(WorkloadStat::getUserId, userIds)
                        .between(WorkloadStat::getStatDate, startDate, endDate)
        );
        return aggregateGroupStats(stats, groupId);
    }

    /**
     * Aggregate stats for entire department (admin only).
     */
    public Map<String, Object> getDepartmentStats(LocalDate startDate, LocalDate endDate) {
        List<WorkloadStat> stats = workloadStatMapper.selectList(
                new LambdaQueryWrapper<WorkloadStat>()
                        .between(WorkloadStat::getStatDate, startDate, endDate)
        );
        return aggregateDepartmentStats(stats);
    }

    /**
     * Daily trend data points for chart rendering.
     */
    public List<Map<String, Object>> getTrendData(Long userId, LocalDate startDate, LocalDate endDate) {
        List<WorkloadStat> stats = workloadStatMapper.selectList(
                new LambdaQueryWrapper<WorkloadStat>()
                        .eq(WorkloadStat::getUserId, userId)
                        .between(WorkloadStat::getStatDate, startDate, endDate)
                        .orderByAsc(WorkloadStat::getStatDate)
        );

        Map<LocalDate, List<WorkloadStat>> groupedByDate = stats.stream()
                .collect(Collectors.groupingBy(WorkloadStat::getStatDate));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<LocalDate, List<WorkloadStat>> entry : groupedByDate.entrySet()) {
            Map<String, Object> point = new HashMap<>();
            int totalCount = entry.getValue().stream()
                    .mapToInt(s -> s.getTreatmentCount() != null ? s.getTreatmentCount() : 0).sum();
            int totalPatients = entry.getValue().stream()
                    .mapToInt(s -> s.getPatientCount() != null ? s.getPatientCount() : 0).sum();
            point.put("date", entry.getKey().toString());
            point.put("count", totalCount);
            point.put("patients", totalPatients);
            result.add(point);
        }
        result.sort(Comparator.comparing(m -> (String) m.get("date")));
        return result;
    }

    /**
     * Export all workload stats for a date range, enriched with user names.
     * Optionally filtered by userId.
     */
    public List<Map<String, Object>> exportStats(Long userId, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<WorkloadStat> wrapper = new LambdaQueryWrapper<WorkloadStat>()
                .between(WorkloadStat::getStatDate, startDate, endDate);
        if (userId != null) {
            wrapper.eq(WorkloadStat::getUserId, userId);
        }
        wrapper.orderByAsc(WorkloadStat::getStatDate)
               .orderByAsc(WorkloadStat::getUserId);

        List<WorkloadStat> stats = workloadStatMapper.selectList(wrapper);

        List<Long> userIds = stats.stream()
                .map(WorkloadStat::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, String> userMap = userIds.isEmpty()
                ? Collections.emptyMap()
                : userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(User::getId, User::getRealName, (a, b) -> a));

        return stats.stream().map(s -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", s.getStatDate() != null ? s.getStatDate().toString() : "");
            row.put("realName", userMap.getOrDefault(s.getUserId(), ""));
            row.put("treatmentCount", s.getTreatmentCount());
            row.put("patientCount", s.getPatientCount());
            row.put("treatmentType", s.getTreatmentType());
            return row;
        }).collect(Collectors.toList());
    }

    // ---- Private aggregation helpers ----

    private Map<String, Object> aggregatePersonalStats(List<WorkloadStat> stats, Long userId) {
        int totalTreatment = stats.stream()
                .mapToInt(s -> s.getTreatmentCount() != null ? s.getTreatmentCount() : 0).sum();
        int totalPatients = stats.stream()
                .mapToInt(s -> s.getPatientCount() != null ? s.getPatientCount() : 0).sum();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", userId);
        result.put("totalTreatmentCount", totalTreatment);
        result.put("totalPatientCount", totalPatients);
        result.put("recordCount", stats.size());
        return result;
    }

    private Map<String, Object> aggregateGroupStats(List<WorkloadStat> stats, Long groupId) {
        int totalTreatment = stats.stream()
                .mapToInt(s -> s.getTreatmentCount() != null ? s.getTreatmentCount() : 0).sum();
        int totalPatients = stats.stream()
                .mapToInt(s -> s.getPatientCount() != null ? s.getPatientCount() : 0).sum();
        long distinctUsers = stats.stream().map(WorkloadStat::getUserId).distinct().count();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("groupId", groupId);
        result.put("totalTreatmentCount", totalTreatment);
        result.put("totalPatientCount", totalPatients);
        result.put("recordCount", stats.size());
        result.put("activeUserCount", distinctUsers);
        return result;
    }

    private Map<String, Object> aggregateDepartmentStats(List<WorkloadStat> stats) {
        int totalTreatment = stats.stream()
                .mapToInt(s -> s.getTreatmentCount() != null ? s.getTreatmentCount() : 0).sum();
        int totalPatients = stats.stream()
                .mapToInt(s -> s.getPatientCount() != null ? s.getPatientCount() : 0).sum();
        long distinctUsers = stats.stream().map(WorkloadStat::getUserId).distinct().count();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalTreatmentCount", totalTreatment);
        result.put("totalPatientCount", totalPatients);
        result.put("recordCount", stats.size());
        result.put("activeUserCount", distinctUsers);
        return result;
    }

    private Map<String, Object> emptyStats(Long groupId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("groupId", groupId);
        result.put("totalTreatmentCount", 0);
        result.put("totalPatientCount", 0);
        result.put("recordCount", 0);
        result.put("activeUserCount", 0);
        return result;
    }
}
