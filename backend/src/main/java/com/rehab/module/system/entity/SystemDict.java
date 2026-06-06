package com.rehab.module.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.rehab.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("system_dict")
public class SystemDict extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String dictType;
    private String dictCode;
    private String dictValue;
    private Integer sortOrder;
    private Integer status;
    private String remark;
}
