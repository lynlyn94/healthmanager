package com.rehab.module.treatment.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rehab.common.Result;
import com.rehab.infrastructure.UserContext;
import com.rehab.module.treatment.entity.TreatmentRecord;
import com.rehab.module.treatment.mapper.TreatmentRecordMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TreatmentRecordController {

    private final TreatmentRecordMapper treatmentRecordMapper;

    @GetMapping("/api/v1/patients/{patientId}/records")
    public Result<List<TreatmentRecord>> listByPatient(@PathVariable Long patientId) {
        List<TreatmentRecord> records = treatmentRecordMapper.selectList(new LambdaQueryWrapper<TreatmentRecord>()
                .eq(TreatmentRecord::getPatientId, patientId)
                .orderByDesc(TreatmentRecord::getTreatmentDate));
        return Result.ok(records);
    }

    @GetMapping("/api/v1/treatment-records/export")
    public void exportRecords(@RequestParam(required = false) Long patientId,
                              @RequestParam(required = false) String startDate,
                              @RequestParam(required = false) String endDate,
                              HttpServletResponse response) throws IOException {
        LambdaQueryWrapper<TreatmentRecord> wrapper = new LambdaQueryWrapper<>();
        if (patientId != null) {
            wrapper.eq(TreatmentRecord::getPatientId, patientId);
        }
        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge(TreatmentRecord::getTreatmentDate, LocalDate.parse(startDate));
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le(TreatmentRecord::getTreatmentDate, LocalDate.parse(endDate));
        }
        wrapper.orderByDesc(TreatmentRecord::getTreatmentDate);
        List<TreatmentRecord> records = treatmentRecordMapper.selectList(wrapper);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String filename = URLEncoder.encode("治疗记录.xlsx", "UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=" + filename);

        List<Map<String, Object>> dataList = records.stream().map(r -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("患者ID", r.getPatientId());
            row.put("治疗师ID", r.getTherapistId());
            row.put("治疗日期", r.getTreatmentDate() != null ? r.getTreatmentDate().toString() : "");
            row.put("治疗项目", r.getTreatmentItem());
            row.put("时长(分钟)", r.getDuration());
            row.put("备注", r.getNote());
            return row;
        }).collect(Collectors.toList());

        EasyExcel.write(response.getOutputStream())
                .head(treatmentRecordHead())
                .sheet("治疗记录")
                .doWrite(dataList);

        log.info("导出治疗记录: patientId={}, count={}", patientId, records.size());
    }

    @PostMapping("/api/v1/treatment-records/import")
    public Result<Map<String, Object>> importRecords(@RequestParam("file") MultipartFile file) throws IOException {
        List<TreatmentRecord> records = new ArrayList<>();
        EasyExcel.read(file.getInputStream(), new ReadListener<Map<Integer, String>>() {
            @Override
            public void invoke(Map<Integer, String> row, AnalysisContext context) {
                TreatmentRecord r = new TreatmentRecord();
                r.setPatientId(parseLong(row.get(0)));
                r.setTherapistId(UserContext.getUserId());
                r.setTreatmentDate(parseDate(row.get(2)));
                r.setTreatmentItem(row.get(3));
                String durStr = row.get(4);
                if (durStr != null && !durStr.isEmpty()) {
                    try { r.setDuration(Integer.parseInt(durStr)); } catch (NumberFormatException ignored) {}
                }
                r.setNote(row.get(5));
                records.add(r);
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {}
        }).sheet().doRead();

        int success = 0, fail = 0;
        for (TreatmentRecord r : records) {
            try {
                treatmentRecordMapper.insert(r);
                success++;
            } catch (Exception e) {
                log.warn("Import treatment record failed: {}", e.getMessage());
                fail++;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", records.size());
        result.put("success", success);
        result.put("fail", fail);
        return Result.ok(result);
    }

    private List<List<String>> treatmentRecordHead() {
        List<List<String>> head = new ArrayList<>();
        head.add(Collections.singletonList("患者ID"));
        head.add(Collections.singletonList("治疗师ID"));
        head.add(Collections.singletonList("治疗日期"));
        head.add(Collections.singletonList("治疗项目"));
        head.add(Collections.singletonList("时长(分钟)"));
        head.add(Collections.singletonList("备注"));
        return head;
    }

    private Long parseLong(String s) {
        if (s == null || s.isEmpty()) return null;
        try { return Long.parseLong(s.trim()); } catch (NumberFormatException e) { return null; }
    }

    private LocalDate parseDate(String s) {
        if (s == null || s.isEmpty()) return null;
        try { return LocalDate.parse(s.trim()); } catch (Exception e) { return null; }
    }
}
