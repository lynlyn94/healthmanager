package com.rehab.module.workload.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("workload_stat")
public class WorkloadStat {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private LocalDate statDate;
    private Integer treatmentCount;
    private Integer patientCount;
    private String treatmentType;
    @TableField(fill = FieldFill.INSERT)
    private Integer isDeleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
