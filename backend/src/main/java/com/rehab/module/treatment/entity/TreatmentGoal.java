package com.rehab.module.treatment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.rehab.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("treatment_goal")
public class TreatmentGoal extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long patientId;
    private String goalType;
    private String content;
    private LocalDate targetDate;
    private String status;
    private Long creatorId;
}
