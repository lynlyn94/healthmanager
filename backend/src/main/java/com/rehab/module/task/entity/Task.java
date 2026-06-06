package com.rehab.module.task.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.rehab.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("task")
public class Task extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long patientId;
    private Long orderId;
    private Long therapistId;
    private Long groupId;
    private LocalDate taskDate;
    private String timeSlot;
    private String treatmentItem;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime verificationTime;
    private LocalDateTime revokeTime;
    private String revokeReason;
    private String note;

    @Version
    private Integer version;
}
