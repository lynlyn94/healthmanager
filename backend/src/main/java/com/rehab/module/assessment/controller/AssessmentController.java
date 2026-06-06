package com.rehab.module.assessment.controller;

import com.rehab.common.Result;
import com.rehab.module.assessment.entity.Assessment;
import com.rehab.module.assessment.entity.AssessmentTemplate;
import com.rehab.module.assessment.service.AssessmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/assessments")
@RequiredArgsConstructor
public class AssessmentController {

    private final AssessmentService assessmentService;

    // ==================== Assessment ====================

    @GetMapping
    public Result<List<Assessment>> listByPatient(@RequestParam Long patientId) {
        return Result.ok(assessmentService.listByPatient(patientId));
    }

    @GetMapping("/{id}")
    public Result<Assessment> getById(@PathVariable Long id) {
        return Result.ok(assessmentService.getById(id));
    }

    @PostMapping
    public Result<Assessment> create(@RequestBody Assessment assessment) {
        return Result.ok(assessmentService.create(assessment));
    }

    @PutMapping("/{id}")
    public Result<Assessment> update(@PathVariable Long id, @RequestBody Assessment assessment) {
        assessment.setId(id);
        return Result.ok(assessmentService.update(assessment));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        assessmentService.deleteAssessment(id);
        return Result.ok();
    }

    // ==================== AssessmentTemplate ====================

    @GetMapping("/templates")
    public Result<List<AssessmentTemplate>> listTemplates() {
        return Result.ok(assessmentService.listTemplates());
    }

    @GetMapping("/templates/{id}")
    public Result<AssessmentTemplate> getTemplateById(@PathVariable Long id) {
        return Result.ok(assessmentService.getTemplateById(id));
    }

    @PostMapping("/templates")
    public Result<AssessmentTemplate> createTemplate(@RequestBody AssessmentTemplate template) {
        return Result.ok(assessmentService.createTemplate(template));
    }

    @PutMapping("/templates/{id}")
    public Result<AssessmentTemplate> updateTemplate(@PathVariable Long id, @RequestBody AssessmentTemplate template) {
        template.setId(id);
        return Result.ok(assessmentService.updateTemplate(template));
    }
}
