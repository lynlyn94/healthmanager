package com.rehab.module.patient.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rehab.common.PageResult;
import com.rehab.common.exception.BusinessException;
import com.rehab.infrastructure.UserContext;
import com.rehab.module.auth.entity.User;
import com.rehab.module.auth.mapper.UserMapper;
import com.rehab.module.patient.entity.Patient;
import com.rehab.module.patient.mapper.PatientMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PatientService {

    private final PatientMapper patientMapper;
    private final UserMapper userMapper;

    public static final String STATUS_IN_HOSPITAL = "IN_HOSPITAL";
    public static final String STATUS_DISCHARGED = "DISCHARGED";

    /**
     * List patients with filtering and pagination.
     * Data scope:
     *   - ADMIN / DOCTOR: see all patients, viewScope is ignored
     *   - THERAPIST / NURSE: default "my" (own patients), can switch to "group" or "all"
     */
    public PageResult<Patient> listPatients(String keyword, String status, String viewScope,
                                            long page, long size) {
        Long userId = UserContext.getUserId();
        String role = UserContext.getRole();
        Long groupId = UserContext.getGroupId();

        LambdaQueryWrapper<Patient> wrapper = new LambdaQueryWrapper<>();

        // Keyword filter: match name, inpatientNo, or bedNo
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w
                    .like(Patient::getName, keyword)
                    .or()
                    .like(Patient::getInpatientNo, keyword)
                    .or()
                    .like(Patient::getBedNo, keyword));
        }

        // Status filter
        if (status != null && !status.isBlank()) {
            wrapper.eq(Patient::getStatus, status);
        }

        // Data scope filter
        applyDataScope(wrapper, userId, role, groupId, viewScope);

        // Default ordering: newest first
        wrapper.orderByDesc(Patient::getCreateTime);

        Page<Patient> mpPage = new Page<>(page, size);
        patientMapper.selectPage(mpPage, wrapper);

        return PageResult.of(mpPage.getTotal(), page, size, mpPage.getRecords());
    }

    private void applyDataScope(LambdaQueryWrapper<Patient> wrapper, Long userId,
                                 String role, Long groupId, String viewScope) {
        // Admin and doctor can see all patients
        if ("ADMIN".equals(role) || "DOCTOR".equals(role)) {
            return;
        }

        // Explicit scope: view all
        if ("all".equals(viewScope)) {
            return;
        }

        // View patients whose attending therapist is in the same group
        if ("group".equals(viewScope) && groupId != null) {
            wrapper.apply("attending_therapist_id IN (SELECT id FROM user WHERE group_id = {0})", groupId);
        } else {
            // Default "my" scope: patients assigned to current user
            wrapper.eq(Patient::getAttendingTherapistId, userId);
        }
    }

    /**
     * Get patient detail by ID.
     */
    public Patient getPatientById(Long id) {
        Patient patient = patientMapper.selectById(id);
        if (patient == null) {
            throw new BusinessException("患者不存在");
        }
        return patient;
    }

    /**
     * Create a new patient. Defaults status to IN_HOSPITAL.
     */
    @Transactional
    public Patient createPatient(Patient patient) {
        if (patient.getStatus() == null) {
            patient.setStatus(STATUS_IN_HOSPITAL);
        }
        patientMapper.insert(patient);
        log.info("Created patient: id={}, name={}", patient.getId(), patient.getName());
        return patient;
    }

    /**
     * Update patient information.
     */
    @Transactional
    public Patient updatePatient(Patient patient) {
        Patient existing = patientMapper.selectById(patient.getId());
        if (existing == null) {
            throw new BusinessException("患者不存在");
        }
        patientMapper.updateById(patient);
        log.info("Updated patient: id={}", patient.getId());
        return patientMapper.selectById(patient.getId());
    }

    /**
     * Discharge a patient: sets status to DISCHARGED and fills dischargeDate.
     */
    @Transactional
    public void dischargePatient(Long id) {
        Patient patient = patientMapper.selectById(id);
        if (patient == null) {
            throw new BusinessException("患者不存在");
        }
        if (STATUS_DISCHARGED.equals(patient.getStatus())) {
            throw new BusinessException("患者已出院，无需重复操作");
        }
        patient.setStatus(STATUS_DISCHARGED);
        patient.setDischargeDate(LocalDate.now());
        patientMapper.updateById(patient);
        log.info("Discharged patient: id={}, name={}, dischargeDate={}",
                patient.getId(), patient.getName(), patient.getDischargeDate());
    }

    /**
     * Export patients with therapist names, optionally filtered by status, keyword, and view scope.
     * Returns a list of LinkedHashMap rows ready for EasyExcel writing.
     */
    public List<Map<String, Object>> exportPatients(String keyword, String status, String viewScope) {
        Long userId = UserContext.getUserId();
        String role = UserContext.getRole();
        Long groupId = UserContext.getGroupId();

        LambdaQueryWrapper<Patient> wrapper = new LambdaQueryWrapper<>();

        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w
                    .like(Patient::getName, keyword)
                    .or()
                    .like(Patient::getInpatientNo, keyword)
                    .or()
                    .like(Patient::getBedNo, keyword));
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(Patient::getStatus, status);
        }

        applyDataScopeForExport(wrapper, userId, role, groupId, viewScope);
        wrapper.orderByDesc(Patient::getCreateTime);

        List<Patient> patients = patientMapper.selectList(wrapper);

        Set<Long> therapistIds = patients.stream()
                .map(Patient::getAttendingTherapistId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> therapistMap = therapistIds.isEmpty()
                ? Collections.emptyMap()
                : userMapper.selectBatchIds(therapistIds).stream()
                        .collect(Collectors.toMap(User::getId, User::getRealName, (a, b) -> a));

        return patients.stream().map(p -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", p.getName());
            row.put("gender", p.getGender() != null && p.getGender() == 1 ? "男" : "女");
            row.put("age", p.getAge());
            row.put("inpatientNo", p.getInpatientNo());
            row.put("bedNo", p.getBedNo());
            row.put("admissionDate", p.getAdmissionDate() != null ? p.getAdmissionDate().toString() : "");
            row.put("diagnosis", p.getDiagnosis());
            row.put("status", STATUS_IN_HOSPITAL.equals(p.getStatus()) ? "在院" : "已出院");
            row.put("therapistName", therapistMap.getOrDefault(p.getAttendingTherapistId(), ""));
            return row;
        }).collect(Collectors.toList());
    }

    private void applyDataScopeForExport(LambdaQueryWrapper<Patient> wrapper, Long userId,
                                          String role, Long groupId, String viewScope) {
        if ("ADMIN".equals(role) || "DOCTOR".equals(role)) {
            return;
        }
        if ("all".equals(viewScope)) {
            return;
        }
        if ("group".equals(viewScope) && groupId != null) {
            wrapper.apply("attending_therapist_id IN (SELECT id FROM user WHERE group_id = {0})", groupId);
        } else {
            wrapper.eq(Patient::getAttendingTherapistId, userId);
        }
    }
}
