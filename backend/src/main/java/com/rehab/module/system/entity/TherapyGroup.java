package com.rehab.module.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.rehab.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("therapy_group")
public class TherapyGroup extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String groupName;
    private Long leaderId;
    private String description;
}
