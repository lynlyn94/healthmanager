package com.rehab.module.assessment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.rehab.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("assessment_template")
public class AssessmentTemplate extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String templateName;
    private String abbreviation;
    private String category;
    private String items;
    private String scoringRule;
    private BigDecimal maxScore;
    private Integer status;
}
