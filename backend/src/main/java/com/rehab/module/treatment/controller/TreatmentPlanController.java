package com.rehab.module.treatment.controller;

import com.rehab.common.Result;
import com.rehab.infrastructure.UserContext;
import com.rehab.module.treatment.entity.TreatmentPlan;
import com.rehab.module.treatment.service.TreatmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/treatment-plans")
@RequiredArgsConstructor
public class TreatmentPlanController {

    private final TreatmentService treatmentService;

    @GetMapping
    public Result<List<TreatmentPlan>> listByPatient(@RequestParam Long patientId) {
        return Result.ok(treatmentService.listPlansByPatient(patientId));
    }

    @PostMapping
    public Result<TreatmentPlan> create(@RequestBody TreatmentPlan plan) {
        return Result.ok(treatmentService.createPlan(plan));
    }

    @PutMapping("/{id}")
    public Result<TreatmentPlan> update(@PathVariable Long id, @RequestBody TreatmentPlan plan) {
        plan.setId(id);
        return Result.ok(treatmentService.updatePlan(plan));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        treatmentService.deletePlan(id);
        return Result.ok();
    }

    @PostMapping("/{id}/submit")
    public Result<TreatmentPlan> submit(@PathVariable Long id) {
        return Result.ok(treatmentService.submitPlan(id));
    }

    @PostMapping("/{id}/review")
    public Result<TreatmentPlan> review(@PathVariable Long id, @RequestBody Map<String, String> params) {
        String reviewComment = params.get("reviewComment");
        return Result.ok(treatmentService.reviewPlan(id, UserContext.getUserId(), reviewComment));
    }
}
