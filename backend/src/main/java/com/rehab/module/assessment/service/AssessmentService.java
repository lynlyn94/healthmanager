package com.rehab.module.assessment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rehab.common.exception.BusinessException;
import com.rehab.infrastructure.UserContext;
import com.rehab.module.assessment.entity.Assessment;
import com.rehab.module.assessment.entity.AssessmentTemplate;
import com.rehab.module.assessment.mapper.AssessmentMapper;
import com.rehab.module.assessment.mapper.AssessmentTemplateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssessmentService {

    private final AssessmentMapper assessmentMapper;
    private final AssessmentTemplateMapper assessmentTemplateMapper;

    // ==================== Assessment ====================

    public List<Assessment> listByPatient(Long patientId) {
        return assessmentMapper.selectList(new LambdaQueryWrapper<Assessment>()
                .eq(Assessment::getPatientId, patientId)
                .orderByDesc(Assessment::getAssessDate));
    }

    public Assessment getById(Long id) {
        Assessment assessment = assessmentMapper.selectById(id);
        if (assessment == null) {
            throw new BusinessException("评估记录不存在");
        }
        return assessment;
    }

    @Transactional
    public Assessment create(Assessment assessment) {
        assessment.setAssessorId(UserContext.getUserId());
        assessmentMapper.insert(assessment);
        log.info("创建评估记录: id={}, patientId={}, templateId={}", assessment.getId(), assessment.getPatientId(), assessment.getTemplateId());
        return assessment;
    }

    @Transactional
    public Assessment update(Assessment assessment) {
        Assessment existing = assessmentMapper.selectById(assessment.getId());
        if (existing == null) {
            throw new BusinessException("评估记录不存在");
        }
        assessmentMapper.updateById(assessment);
        log.info("更新评估记录: id={}", assessment.getId());
        return assessmentMapper.selectById(assessment.getId());
    }

    // ==================== AssessmentTemplate ====================

    @Cacheable(value = "template", key = "'allTemplates'")
    public List<AssessmentTemplate> listTemplates() {
        return assessmentTemplateMapper.selectList(new LambdaQueryWrapper<AssessmentTemplate>()
                .orderByAsc(AssessmentTemplate::getCategory)
                .orderByAsc(AssessmentTemplate::getTemplateName));
    }

    public AssessmentTemplate getTemplateById(Long id) {
        AssessmentTemplate template = assessmentTemplateMapper.selectById(id);
        if (template == null) {
            throw new BusinessException("评估模板不存在");
        }
        return template;
    }

    @Transactional
    @CacheEvict(value = "template", allEntries = true)
    public AssessmentTemplate createTemplate(AssessmentTemplate template) {
        assessmentTemplateMapper.insert(template);
        log.info("创建评估模板: id={}, name={}", template.getId(), template.getTemplateName());
        return template;
    }

    @Transactional
    @CacheEvict(value = "template", allEntries = true)
    public AssessmentTemplate updateTemplate(AssessmentTemplate template) {
        AssessmentTemplate existing = assessmentTemplateMapper.selectById(template.getId());
        if (existing == null) {
            throw new BusinessException("评估模板不存在");
        }
        assessmentTemplateMapper.updateById(template);
        log.info("更新评估模板: id={}", template.getId());
        return assessmentTemplateMapper.selectById(template.getId());
    }

    @Transactional
    public void deleteAssessment(Long id) {
        Assessment existing = assessmentMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("评估记录不存在");
        }
        assessmentMapper.deleteById(id);
        log.info("删除评估记录: id={}", id);
    }

    @Transactional
    @CacheEvict(value = "template", allEntries = true)
    public void toggleTemplateStatus(Long id) {
        AssessmentTemplate template = assessmentTemplateMapper.selectById(id);
        if (template == null) {
            throw new BusinessException("评估模板不存在");
        }
        Integer newStatus = (template.getStatus() != null && template.getStatus() == 1) ? 0 : 1;
        template.setStatus(newStatus);
        assessmentTemplateMapper.updateById(template);
        log.info("切换评估模板状态: id={}, status={}", id, newStatus);
    }
}
