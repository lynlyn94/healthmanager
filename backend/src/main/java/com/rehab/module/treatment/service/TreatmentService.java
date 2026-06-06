package com.rehab.module.treatment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rehab.common.exception.BusinessException;
import com.rehab.infrastructure.UserContext;
import com.rehab.module.treatment.entity.TreatmentGoal;
import com.rehab.module.treatment.entity.TreatmentPlan;
import com.rehab.module.treatment.mapper.TreatmentGoalMapper;
import com.rehab.module.treatment.mapper.TreatmentPlanMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TreatmentService {

    private final TreatmentGoalMapper treatmentGoalMapper;
    private final TreatmentPlanMapper treatmentPlanMapper;

    // ==================== TreatmentGoal ====================

    public List<TreatmentGoal> listGoalsByPatient(Long patientId) {
        return treatmentGoalMapper.selectList(new LambdaQueryWrapper<TreatmentGoal>()
                .eq(TreatmentGoal::getPatientId, patientId)
                .orderByAsc(TreatmentGoal::getGoalType)
                .orderByDesc(TreatmentGoal::getTargetDate));
    }

    @Transactional
    public TreatmentGoal createGoal(TreatmentGoal goal) {
        goal.setCreatorId(UserContext.getUserId());
        if (goal.getStatus() == null) {
            goal.setStatus("IN_PROGRESS");
        }
        treatmentGoalMapper.insert(goal);
        log.info("创建治疗目标: id={}, patientId={}, type={}", goal.getId(), goal.getPatientId(), goal.getGoalType());
        return goal;
    }

    @Transactional
    public TreatmentGoal updateGoal(TreatmentGoal goal) {
        TreatmentGoal existing = treatmentGoalMapper.selectById(goal.getId());
        if (existing == null) {
            throw new BusinessException("治疗目标不存在");
        }
        treatmentGoalMapper.updateById(goal);
        log.info("更新治疗目标: id={}", goal.getId());
        return treatmentGoalMapper.selectById(goal.getId());
    }

    @Transactional
    public void deleteGoal(Long id) {
        TreatmentGoal existing = treatmentGoalMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("治疗目标不存在");
        }
        treatmentGoalMapper.deleteById(id);
        log.info("删除治疗目标: id={}", id);
    }

    // ==================== TreatmentPlan ====================

    public List<TreatmentPlan> listPlansByPatient(Long patientId) {
        return treatmentPlanMapper.selectList(new LambdaQueryWrapper<TreatmentPlan>()
                .eq(TreatmentPlan::getPatientId, patientId)
                .orderByDesc(TreatmentPlan::getPeriodStart));
    }

    @Transactional
    public TreatmentPlan createPlan(TreatmentPlan plan) {
        plan.setCreatorId(UserContext.getUserId());
        if (plan.getStatus() == null) {
            plan.setStatus("DRAFT");
        }
        plan.setTreatmentItems(toJsonArray(plan.getTreatmentItems()));
        treatmentPlanMapper.insert(plan);
        log.info("创建治疗计划: id={}, patientId={}, name={}", plan.getId(), plan.getPatientId(), plan.getPlanName());
        return plan;
    }

    @Transactional
    public TreatmentPlan updatePlan(TreatmentPlan plan) {
        TreatmentPlan existing = treatmentPlanMapper.selectById(plan.getId());
        if (existing == null) {
            throw new BusinessException("治疗计划不存在");
        }
        plan.setTreatmentItems(toJsonArray(plan.getTreatmentItems()));
        treatmentPlanMapper.updateById(plan);
        log.info("更新治疗计划: id={}", plan.getId());
        return treatmentPlanMapper.selectById(plan.getId());
    }

    // Convert comma-separated string to JSON array, or wrap single value
    private String toJsonArray(String input) {
        if (input == null || input.isBlank()) return "[]";
        String trimmed = input.trim();
        if (trimmed.startsWith("[")) return trimmed; // already JSON
        String[] parts = trimmed.split("[,，]");
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(parts[i].trim()).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }

    @Transactional
    public void deletePlan(Long id) {
        TreatmentPlan existing = treatmentPlanMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("治疗计划不存在");
        }
        treatmentPlanMapper.deleteById(id);
        log.info("删除治疗计划: id={}", id);
    }

    @Transactional
    public TreatmentPlan submitPlan(Long id) {
        TreatmentPlan plan = treatmentPlanMapper.selectById(id);
        if (plan == null) {
            throw new BusinessException("治疗计划不存在");
        }
        if (!"DRAFT".equals(plan.getStatus())) {
            throw new BusinessException("只有草稿状态的计划可以提交");
        }
        plan.setStatus("SUBMITTED");
        plan.setSubmitTime(LocalDateTime.now());
        treatmentPlanMapper.updateById(plan);
        log.info("提交治疗计划: id={}", id);
        return plan;
    }

    @Transactional
    public TreatmentPlan reviewPlan(Long id, Long reviewerId, String reviewComment) {
        TreatmentPlan plan = treatmentPlanMapper.selectById(id);
        if (plan == null) {
            throw new BusinessException("治疗计划不存在");
        }
        if (!"SUBMITTED".equals(plan.getStatus())) {
            throw new BusinessException("只有已提交状态的计划可以审核");
        }
        plan.setStatus("APPROVED");
        plan.setReviewerId(reviewerId);
        plan.setReviewComment(reviewComment);
        treatmentPlanMapper.updateById(plan);
        log.info("审核治疗计划: id={}, reviewerId={}", id, reviewerId);
        return plan;
    }

    public List<TreatmentPlan> listPendingReview() {
        return treatmentPlanMapper.selectList(
                new LambdaQueryWrapper<TreatmentPlan>()
                        .eq(TreatmentPlan::getStatus, "SUBMITTED")
                        .orderByDesc(TreatmentPlan::getSubmitTime));
    }

    // Plans approved by the current doctor, waiting for orders
    public List<TreatmentPlan> listApprovedByReviewer(Long reviewerId) {
        return treatmentPlanMapper.selectList(
                new LambdaQueryWrapper<TreatmentPlan>()
                        .eq(TreatmentPlan::getStatus, "APPROVED")
                        .eq(TreatmentPlan::getReviewerId, reviewerId)
                        .orderByDesc(TreatmentPlan::getCreateTime));
    }

    @Transactional
    public void rejectPlan(Long id, String reason) {
        TreatmentPlan plan = treatmentPlanMapper.selectById(id);
        if (plan == null) {
            throw new BusinessException("治疗计划不存在");
        }
        if (!"SUBMITTED".equals(plan.getStatus())) {
            throw new BusinessException("只有已提交状态的计划可以退回");
        }
        plan.setStatus("DRAFT");
        plan.setReviewComment("退回原因: " + (reason != null ? reason : ""));
        treatmentPlanMapper.updateById(plan);
        log.info("退回治疗计划: id={}, reason={}", id, reason);
    }

    @Transactional
    public void markPlanOrdered(Long planId) {
        TreatmentPlan plan = treatmentPlanMapper.selectById(planId);
        if (plan != null && "APPROVED".equals(plan.getStatus())) {
            plan.setStatus("ORDERED");
            treatmentPlanMapper.updateById(plan);
            log.info("治疗方案已开医嘱: id={}", planId);
        }
    }
}
