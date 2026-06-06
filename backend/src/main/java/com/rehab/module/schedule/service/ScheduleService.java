package com.rehab.module.schedule.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rehab.infrastructure.UserContext;
import com.rehab.module.auth.entity.User;
import com.rehab.module.auth.mapper.UserMapper;
import com.rehab.module.schedule.entity.PatientSchedule;
import com.rehab.module.schedule.mapper.PatientScheduleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final PatientScheduleMapper patientScheduleMapper;
    private final UserMapper userMapper;

    /**
     * Query schedules for a patient within a date range.
     */
    public List<PatientSchedule> getByPatient(Long patientId, LocalDate startDate, LocalDate endDate) {
        return patientScheduleMapper.selectList(
                new LambdaQueryWrapper<PatientSchedule>()
                        .eq(PatientSchedule::getPatientId, patientId)
                        .between(PatientSchedule::getScheduleDate, startDate, endDate)
                        .orderByAsc(PatientSchedule::getScheduleDate)
                        .orderByAsc(PatientSchedule::getTimeSlot)
        );
    }

    /**
     * Query schedules for the current user (by therapistId).
     */
    public List<PatientSchedule> getMySchedule(LocalDate startDate, LocalDate endDate) {
        Long userId = UserContext.getUserId();
        return patientScheduleMapper.selectList(
                new LambdaQueryWrapper<PatientSchedule>()
                        .eq(PatientSchedule::getTherapistId, userId)
                        .between(PatientSchedule::getScheduleDate, startDate, endDate)
                        .orderByAsc(PatientSchedule::getScheduleDate)
                        .orderByAsc(PatientSchedule::getTimeSlot)
        );
    }

    /**
     * Query schedules for all therapists in a group.
     */
    public List<PatientSchedule> getGroupSchedule(Long groupId, LocalDate startDate, LocalDate endDate) {
        List<Long> userIds = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .eq(User::getGroupId, groupId)
        ).stream().map(User::getId).collect(Collectors.toList());

        if (userIds.isEmpty()) {
            return Collections.emptyList();
        }

        return patientScheduleMapper.selectList(
                new LambdaQueryWrapper<PatientSchedule>()
                        .in(PatientSchedule::getTherapistId, userIds)
                        .between(PatientSchedule::getScheduleDate, startDate, endDate)
                        .orderByAsc(PatientSchedule::getScheduleDate)
                        .orderByAsc(PatientSchedule::getTimeSlot)
        );
    }
}
