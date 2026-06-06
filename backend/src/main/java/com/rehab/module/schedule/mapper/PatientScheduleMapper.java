package com.rehab.module.schedule.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rehab.module.schedule.entity.PatientSchedule;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PatientScheduleMapper extends BaseMapper<PatientSchedule> {
}
