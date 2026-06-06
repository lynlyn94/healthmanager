package com.rehab.module.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rehab.module.task.entity.Task;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface TaskMapper extends BaseMapper<Task> {

    @Select("SELECT COUNT(*) > 0 FROM task WHERE order_id = #{orderId} AND task_date = #{taskDate} AND time_slot = #{timeSlot} AND is_deleted = 0")
    boolean existsByOrderAndDateAndSlot(@Param("orderId") Long orderId,
                                        @Param("taskDate") LocalDate taskDate,
                                        @Param("timeSlot") String timeSlot);

    @Select("SELECT DISTINCT treatment_item FROM task WHERE is_deleted = 0 ORDER BY treatment_item")
    List<String> selectDistinctTreatmentItems();

    @Select("SELECT COUNT(*) > 0 FROM task WHERE therapist_id = #{therapistId} AND task_date = #{taskDate} AND time_slot = #{timeSlot} AND is_deleted = 0")
    boolean existsConflict(@Param("therapistId") Long therapistId,
                           @Param("taskDate") LocalDate taskDate,
                           @Param("timeSlot") String timeSlot);
}
