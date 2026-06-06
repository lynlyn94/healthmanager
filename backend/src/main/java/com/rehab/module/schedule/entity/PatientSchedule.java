package com.rehab.module.schedule.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.rehab.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("patient_schedule")
public class PatientSchedule extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long patientId;
    private LocalDate scheduleDate;
    private String timeSlot;
    private String eventType;
    private Long sourceId;
    private Long therapistId;
    private String title;
    private String description;
    private String status;
}
