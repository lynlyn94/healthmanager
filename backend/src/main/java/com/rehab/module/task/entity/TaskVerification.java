package com.rehab.module.task.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("task_verification")
public class TaskVerification {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long verifierId;
    private LocalDateTime verifyTime;
    private Integer revoked;
    private LocalDateTime revokeTime;
    private String revokeReason;
    @TableField(fill = FieldFill.INSERT)
    private Integer isDeleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
