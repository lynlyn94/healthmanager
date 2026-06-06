package com.rehab.module.treatment.controller;

import com.rehab.common.Result;
import com.rehab.module.treatment.entity.TreatmentGoal;
import com.rehab.module.treatment.service.TreatmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TreatmentGoalController {

    private final TreatmentService treatmentService;

    @GetMapping("/api/v1/patients/{patientId}/goals")
    public Result<List<TreatmentGoal>> listByPatient(@PathVariable Long patientId) {
        return Result.ok(treatmentService.listGoalsByPatient(patientId));
    }

    @PostMapping("/api/v1/patients/{patientId}/goals")
    public Result<TreatmentGoal> create(@PathVariable Long patientId, @RequestBody TreatmentGoal goal) {
        goal.setPatientId(patientId);
        return Result.ok(treatmentService.createGoal(goal));
    }

    @PutMapping("/api/v1/goals/{id}")
    public Result<TreatmentGoal> update(@PathVariable Long id, @RequestBody TreatmentGoal goal) {
        goal.setId(id);
        return Result.ok(treatmentService.updateGoal(goal));
    }

    @DeleteMapping("/api/v1/goals/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        treatmentService.deleteGoal(id);
        return Result.ok();
    }
}
