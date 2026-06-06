package com.rehab.module.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rehab.module.task.entity.Task;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;

@Mapper
public interface TaskMapper extends BaseMapper<Task> {

    @Select("SELECT COUNT(*) > 0 FROM task WHERE order_id = #{orderId} AND task_date = #{taskDate} AND time_slot = #{timeSlot} AND is_deleted = 0")
    boolean existsByOrderAndDateAndSlot(@Param("orderId") Long orderId,
                                        @Param("taskDate") LocalDate taskDate,
                                        @Param("timeSlot") String timeSlot);
}
