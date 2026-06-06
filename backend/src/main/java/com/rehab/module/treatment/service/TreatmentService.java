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
        treatmentPlanMapper.updateById(plan);
        log.info("更新治疗计划: id={}", plan.getId());
        return treatmentPlanMapper.selectById(plan.getId());
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
        plan.setStatus("REVIEWED");
        plan.setReviewerId(reviewerId);
        plan.setReviewComment(reviewComment);
        treatmentPlanMapper.updateById(plan);
        log.info("审核治疗计划: id={}, reviewerId={}", id, reviewerId);
        return plan;
    }
}
