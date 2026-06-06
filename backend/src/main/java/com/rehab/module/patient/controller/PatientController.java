package com.rehab.module.patient.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.rehab.common.PageResult;
import com.rehab.common.Result;
import com.rehab.module.patient.entity.Patient;
import com.rehab.module.patient.service.PatientService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.*;

@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
@Slf4j
public class PatientController {

    private final PatientService patientService;

    /**
     * GET /api/v1/patients
     * Paginated patient list with optional filters.
     *
     * @param keyword   search by name, inpatientNo, or bedNo
     * @param status    filter by IN_HOSPITAL / DISCHARGED
     * @param viewScope data scope: my (default), group, all
     * @param page      page number, 1-based, default 1
     * @param size      page size, default 10
     */
    @GetMapping
    public Result<PageResult<Patient>> list(@RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String status,
                                            @RequestParam(required = false) String viewScope,
                                            @RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(patientService.listPatients(keyword, status, viewScope, page, size));
    }

    /**
     * GET /api/v1/patients/export
     * Export patient list as Excel file. Must be defined before /{id} to avoid path variable conflict.
     */
    @GetMapping("/export")
    public void export(@RequestParam(required = false) String status,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String viewScope,
                       HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String filename = URLEncoder.encode("患者列表.xlsx", "UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=" + filename);

        List<Map<String, Object>> dataList = patientService.exportPatients(keyword, status, viewScope);
        EasyExcel.write(response.getOutputStream(), Map.class).head(patientHead()).sheet("患者列表").doWrite(dataList);
    }

    /**
     * GET /api/v1/patients/{id}
     * Get patient detail by ID.
     */
    @GetMapping("/{id}")
    public Result<Patient> detail(@PathVariable Long id) {
        return Result.ok(patientService.getPatientById(id));
    }

    /**
     * POST /api/v1/patients
     * Create a new patient.
     */
    @PostMapping
    public Result<Patient> create(@Valid @RequestBody Patient patient) {
        return Result.ok(patientService.createPatient(patient));
    }

    /**
     * PUT /api/v1/patients/{id}
     * Update an existing patient.
     */
    @PutMapping("/{id}")
    public Result<Patient> update(@PathVariable Long id, @Valid @RequestBody Patient patient) {
        patient.setId(id);
        return Result.ok(patientService.updatePatient(patient));
    }

    /**
     * PUT /api/v1/patients/{id}/discharge
     * Discharge a patient: set status to DISCHARGED and record discharge date.
     */
    @PutMapping("/{id}/discharge")
    public Result<Void> discharge(@PathVariable Long id) {
        patientService.dischargePatient(id);
        return Result.ok();
    }

    @PostMapping("/import")
    public Result<Map<String, Object>> importPatients(@RequestParam("file") MultipartFile file) throws IOException {
        List<Patient> patients = new ArrayList<>();
        EasyExcel.read(file.getInputStream(), new ReadListener<Map<Integer, String>>() {
            @Override
            public void invoke(Map<Integer, String> row, AnalysisContext context) {
                Patient p = new Patient();
                p.setName(val(row, 0));
                String gender = val(row, 1);
                p.setGender("男".equals(gender) ? 1 : 0);
                String ageStr = val(row, 2);
                if (ageStr != null && !ageStr.isEmpty()) {
                    try { p.setAge(Integer.parseInt(ageStr)); } catch (NumberFormatException ignored) {}
                }
                p.setInpatientNo(val(row, 3));
                p.setBedNo(val(row, 4));
                p.setDiagnosis(val(row, 5));
                p.setStatus("IN_HOSPITAL");
                patients.add(p);
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {}
        }).sheet().doRead();

        int success = 0, fail = 0;
        for (Patient p : patients) {
            try {
                patientService.createPatient(p);
                success++;
            } catch (Exception e) {
                log.warn("Import patient failed: name={}, error={}", p.getName(), e.getMessage());
                fail++;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", patients.size());
        result.put("success", success);
        result.put("fail", fail);
        return Result.ok(result);
    }

    private String val(Map<Integer, String> row, int idx) {
        String v = row.get(idx);
        return v != null ? v.trim() : null;
    }

    private List<List<String>> patientHead() {
        return Arrays.asList(
                Arrays.asList("姓名"),
                Arrays.asList("性别"),
                Arrays.asList("年龄"),
                Arrays.asList("住院号"),
                Arrays.asList("床号"),
                Arrays.asList("入院日期"),
                Arrays.asList("诊断"),
                Arrays.asList("状态"),
                Arrays.asList("负责治疗师"));
    }
}
