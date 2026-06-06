package com.rehab.module.task.service;

import com.rehab.common.exception.BusinessException;
import com.rehab.module.order.entity.MedicalOrder;
import com.rehab.module.order.mapper.MedicalOrderMapper;
import com.rehab.module.schedule.entity.PatientSchedule;
import com.rehab.module.schedule.mapper.PatientScheduleMapper;
import com.rehab.module.task.entity.Task;
import com.rehab.module.task.mapper.TaskMapper;
import com.rehab.module.task.mapper.TaskVerificationMapper;
import com.rehab.module.treatment.mapper.TreatmentRecordMapper;
import com.rehab.module.workload.mapper.WorkloadStatMapper;
import com.rehab.module.auth.entity.User;
import com.rehab.module.auth.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock private TaskMapper taskMapper;
    @Mock private TaskVerificationMapper taskVerificationMapper;
    @Mock private UserMapper userMapper;
    @Mock private WorkloadStatMapper workloadStatMapper;
    @Mock private TreatmentRecordMapper treatmentRecordMapper;
    @Mock private MedicalOrderMapper medicalOrderMapper;
    @Mock private PatientScheduleMapper patientScheduleMapper;

    @InjectMocks
    private TaskService taskService;

    private Task pendingTask;
    private MedicalOrder approvedOrder;

    @BeforeEach
    void setUp() {
        pendingTask = new Task();
        pendingTask.setId(1L);
        pendingTask.setPatientId(10L);
        pendingTask.setTherapistId(2L);
        pendingTask.setTreatmentItem("推拿治疗");
        pendingTask.setStatus("PENDING");
        pendingTask.setTaskDate(LocalDate.now());

        approvedOrder = new MedicalOrder();
        approvedOrder.setId(100L);
        approvedOrder.setPatientId(10L);
        approvedOrder.setTreatmentItem("推拿治疗");
        approvedOrder.setStatus("APPROVED");
        approvedOrder.setPeriodStart(LocalDate.now());
        approvedOrder.setPeriodEnd(LocalDate.now().plusDays(2));
    }

    // ---- createTask ----
    @Test
    void createTask_SetsDefaultStatus() {
        when(taskMapper.insert(any(Task.class))).thenAnswer(inv -> { inv.getArgument(0, Task.class).setId(99L); return 1; });

        Task input = new Task();
        input.setPatientId(10L);
        input.setTreatmentItem("测试");
        Task result = taskService.createTask(input);

        assertThat(result.getStatus()).isEqualTo("PENDING");
        verify(taskMapper).insert(any(Task.class));
    }

    // ---- startTask ----
    @Test
    void startTask_WhenPending_SetsInProgress() {
        when(taskMapper.selectById(1L)).thenReturn(pendingTask);

        taskService.startTask(1L);

        assertThat(pendingTask.getStatus()).isEqualTo("IN_PROGRESS");
        assertThat(pendingTask.getStartTime()).isNotNull();
    }

    @Test
    void startTask_WhenNotPending_ThrowsException() {
        pendingTask.setStatus("VERIFIED");
        when(taskMapper.selectById(1L)).thenReturn(pendingTask);

        assertThatThrownBy(() -> taskService.startTask(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("待办");
    }

    // ---- verifyTask ----
    @Test
    void verifyTask_WhenWrongTherapist_ThrowsException() {
        pendingTask.setStatus("IN_PROGRESS");
        pendingTask.setTherapistId(999L); // different from current user
        when(taskMapper.selectById(1L)).thenReturn(pendingTask);

        assertThatThrownBy(() -> taskService.verifyTask(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("核销");
    }

    // ---- generateTasksFromOrder ----
    @Test
    void generateTasksFromOrder_WhenOrderNotFound_ThrowsException() {
        when(medicalOrderMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> taskService.generateTasksFromOrder(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不存在");
    }

    @Test
    void generateTasksFromOrder_WhenNotApproved_ThrowsException() {
        approvedOrder.setStatus("DRAFT");
        when(medicalOrderMapper.selectById(100L)).thenReturn(approvedOrder);

        assertThatThrownBy(() -> taskService.generateTasksFromOrder(100L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("审核通过");
    }

    @Test
    void generateTasksFromOrder_CreatesTasksForSchedules() {
        when(medicalOrderMapper.selectById(100L)).thenReturn(approvedOrder);

        PatientSchedule s1 = new PatientSchedule();
        s1.setId(1L); s1.setSourceId(100L); s1.setScheduleDate(LocalDate.now());
        s1.setTimeSlot("上午"); s1.setEventType("ORDER"); s1.setStatus("SCHEDULED");
        s1.setTherapistId(2L);

        PatientSchedule s2 = new PatientSchedule();
        s2.setId(2L); s2.setSourceId(100L); s2.setScheduleDate(LocalDate.now().plusDays(1));
        s2.setTimeSlot("上午"); s2.setEventType("ORDER"); s2.setStatus("SCHEDULED");
        s2.setTherapistId(2L);

        when(patientScheduleMapper.selectList(any())).thenReturn(List.of(s1, s2));
        when(taskMapper.existsByOrderAndDateAndSlot(anyLong(), any(), any())).thenReturn(false);

        User therapist = new User();
        therapist.setId(2L); therapist.setGroupId(5L);
        when(userMapper.selectById(2L)).thenReturn(therapist);

        List<Task> created = taskService.generateTasksFromOrder(100L);

        assertThat(created).hasSize(2);
        assertThat(created.get(0).getStatus()).isEqualTo("PENDING");
        assertThat(created.get(0).getOrderId()).isEqualTo(100L);
        assertThat(created.get(0).getGroupId()).isEqualTo(5L);
        verify(taskMapper, times(2)).insert(any(Task.class));
        verify(patientScheduleMapper, times(2)).updateById(any(PatientSchedule.class));
    }

    @Test
    void generateTasksFromOrder_SkipsExistingTasks() {
        when(medicalOrderMapper.selectById(100L)).thenReturn(approvedOrder);

        PatientSchedule s1 = new PatientSchedule();
        s1.setId(1L); s1.setSourceId(100L); s1.setScheduleDate(LocalDate.now());
        s1.setTimeSlot("上午"); s1.setEventType("ORDER"); s1.setStatus("SCHEDULED");
        s1.setTherapistId(2L);

        when(patientScheduleMapper.selectList(any())).thenReturn(List.of(s1));
        when(taskMapper.existsByOrderAndDateAndSlot(100L, LocalDate.now(), "上午")).thenReturn(true);

        List<Task> created = taskService.generateTasksFromOrder(100L);

        assertThat(created).isEmpty();
        verify(taskMapper, never()).insert(any(Task.class));
    }
}
