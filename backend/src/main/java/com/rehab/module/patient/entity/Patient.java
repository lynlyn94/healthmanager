package com.rehab.module.patient.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.rehab.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("patient")
public class Patient extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Integer gender;
    private Integer age;
    private String inpatientNo;
    private String bedNo;
    private LocalDate admissionDate;
    private String diagnosis;
    private String allergyHistory;
    private String contactPhone;
    private String emergencyContact;
    private String emergencyPhone;
    private Long attendingTherapistId;
    private Long attendingDoctorId;
    private String status;
    private LocalDate dischargeDate;
    private String remark;
}
