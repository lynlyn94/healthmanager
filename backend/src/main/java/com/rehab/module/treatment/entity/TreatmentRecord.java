package com.rehab.module.treatment.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("treatment_record")
public class TreatmentRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long patientId;
    private Long taskId;
    private Long therapistId;
    private LocalDate treatmentDate;
    private String treatmentItem;
    private Integer duration;
    private String note;
    @TableField(fill = FieldFill.INSERT)
    private Integer isDeleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
