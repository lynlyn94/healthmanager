package com.rehab.module.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.rehab.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("medical_order")
public class MedicalOrder extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long patientId;
    private Long doctorId;
    private Long planId;
    private String orderType;
    private String treatmentItem;
    private String frequency;
    private Integer dailyCount;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String note;
    private String status;
    private String reviewComment;
}
