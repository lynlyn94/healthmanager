package com.rehab.module.treatment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.rehab.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("treatment_plan")
public class TreatmentPlan extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long patientId;
    private String planName;
    private String treatmentItems;
    private String frequency;
    private Integer dailyCount;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String status;
    private Long creatorId;
    private Long reviewerId;
    private String reviewComment;
    private LocalDateTime submitTime;
}
