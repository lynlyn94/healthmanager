package com.rehab.module.system.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("operation_log")
public class OperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String username;
    private String module;
    private String action;
    private String targetType;
    private Long targetId;
    private String description;
    private String requestIp;
    private String requestUrl;
    private String requestParam;
    private Long costTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
