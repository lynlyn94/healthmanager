package com.rehab.module.task.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rehab.common.PageResult;
import com.rehab.common.exception.BusinessException;
import com.rehab.infrastructure.UserContext;
import com.rehab.module.auth.entity.User;
import com.rehab.module.auth.mapper.UserMapper;
import com.rehab.module.order.entity.MedicalOrder;
import com.rehab.module.order.mapper.MedicalOrderMapper;
import com.rehab.module.schedule.entity.PatientSchedule;
import com.rehab.module.schedule.mapper.PatientScheduleMapper;
import com.rehab.module.task.entity.Task;
import com.rehab.module.task.entity.TaskVerification;
import com.rehab.module.task.mapper.TaskMapper;
import com.rehab.module.task.mapper.TaskVerificationMapper;
import com.rehab.module.treatment.entity.TreatmentRecord;
import com.rehab.module.treatment.mapper.TreatmentRecordMapper;
import com.rehab.module.workload.entity.WorkloadStat;
import com.rehab.module.workload.mapper.WorkloadStatMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskMapper taskMapper;
    private final TaskVerificationMapper taskVerificationMapper;
    private final UserMapper userMapper;
    private final WorkloadStatMapper workloadStatMapper;
    private final TreatmentRecordMapper treatmentRecordMapper;
    private final MedicalOrderMapper medicalOrderMapper;
    private final PatientScheduleMapper patientScheduleMapper;

    private static final String PENDING = "PENDING";
    private static final String IN_PROGRESS = "IN_PROGRESS";
    private static final String VERIFIED = "VERIFIED";
    private static final String REVOKED = "REVOKED";

    // -------------------- listTasks --------------------

    public PageResult<Task> listTasks(Long therapistId, Long groupId, Long patientId,
                                      String status, LocalDate date, int page, int size) {
        Long currentUserId = UserContext.getUserId();
        Long currentGroupId = UserContext.getGroupId();
        boolean isAdmin = UserContext.isAdmin();

        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();

        // Data scope: admin sees all; others see own or group-scoped
        if (!isAdmin) {
            if (groupId != null && Objects.equals(groupId, currentGroupId)) {
                // User scoping to their own group — show that group's tasks
                wrapper.eq(Task::getGroupId, groupId);
            } else {
                // Default: show tasks assigned to the current user
                wrapper.eq(Task::getTherapistId, currentUserId);
            }
        } else {
            // Admin can filter by group or therapist
            if (groupId != null) {
                wrapper.eq(Task::getGroupId, groupId);
            }
            if (therapistId != null) {
                wrapper.eq(Task::getTherapistId, therapistId);
            }
        }

        // Optional filters
        if (patientId != null) {
            wrapper.eq(Task::getPatientId, patientId);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(Task::getStatus, status);
        }
        if (date != null) {
            wrapper.eq(Task::getTaskDate, date);
        }

        wrapper.orderByDesc(Task::getTaskDate).orderByAsc(Task::getTimeSlot);

        Page<Task> mpPage = new Page<>(page, size);
        Page<Task> resultPage = taskMapper.selectPage(mpPage, wrapper);

        return PageResult.of(resultPage.getTotal(), page, size, resultPage.getRecords());
    }

    // -------------------- getById --------------------

    public Task getById(Long id) {
        Task task = taskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        return task;
    }

    // -------------------- createTask --------------------

    public Task createTask(Task task) {
        Long currentUserId = UserContext.getUserId();

        task.setId(null);

        // Set groupId from the creator's group
        task.setGroupId(UserContext.getGroupId());

        // If therapistId is set, verify it belongs to the same group; otherwise assign to self
        if (task.getTherapistId() == null) {
            task.setTherapistId(currentUserId);
        }

        // If no creator (therapist) specified, default to current user
        task.setStatus(PENDING);

        taskMapper.insert(task);
        log.info("Task created: id={}, patientId={}, treatmentItem={}, by userId={}",
                task.getId(), task.getPatientId(), task.getTreatmentItem(), currentUserId);
        return task;
    }

    // -------------------- scheduleTask --------------------

    public void scheduleTask(Long taskId, String timeSlot, Long therapistId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }

        task.setTimeSlot(timeSlot);
        if (therapistId != null) {
            task.setTherapistId(therapistId);
        }

        taskMapper.updateById(task);
        log.info("Task scheduled: id={}, timeSlot={}, therapistId={}", taskId, timeSlot, therapistId);
    }

    // -------------------- startTask --------------------

    public void startTask(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        if (!PENDING.equals(task.getStatus())) {
            throw new BusinessException("只有待办状态的任务才能开始");
        }

        task.setStatus(IN_PROGRESS);
        task.setStartTime(LocalDateTime.now());

        taskMapper.updateById(task);
        log.info("Task started: id={}", taskId);
    }

    // -------------------- verifyTask --------------------

    @Transactional
    public void verifyTask(Long taskId) {
        Long currentUserId = UserContext.getUserId();
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        if (!Objects.equals(task.getTherapistId(), currentUserId)) {
            throw new BusinessException("只有任务执行人才能核销");
        }
        if (!IN_PROGRESS.equals(task.getStatus())) {
            throw new BusinessException("只有进行中的任务才能核销");
        }

        LocalDateTime now = LocalDateTime.now();

        // 1. Update task status to VERIFIED
        task.setStatus(VERIFIED);
        task.setVerificationTime(now);
        taskMapper.updateById(task);

        // 2. Insert TaskVerification record
        TaskVerification verification = new TaskVerification();
        verification.setTaskId(taskId);
        verification.setVerifierId(currentUserId);
        verification.setVerifyTime(now);
        verification.setRevoked(0);
        taskVerificationMapper.insert(verification);

        // 3. Upsert WorkloadStat — today's stat for this user + treatment type
        LocalDate today = LocalDate.now();
        String treatmentType = task.getTreatmentItem();

        LambdaQueryWrapper<WorkloadStat> statWrapper = new LambdaQueryWrapper<>();
        statWrapper.eq(WorkloadStat::getUserId, currentUserId)
                .eq(WorkloadStat::getStatDate, today)
                .eq(WorkloadStat::getTreatmentType, treatmentType);
        WorkloadStat stat = workloadStatMapper.selectOne(statWrapper);

        if (stat == null) {
            stat = new WorkloadStat();
            stat.setUserId(currentUserId);
            stat.setStatDate(today);
            stat.setTreatmentCount(1);
            stat.setPatientCount(1);
            stat.setTreatmentType(treatmentType);
            workloadStatMapper.insert(stat);
        } else {
            stat.setTreatmentCount(stat.getTreatmentCount() + 1);
            // Increment patientCount only if this patient hasn't been treated today
            LambdaQueryWrapper<TreatmentRecord> trWrapper = new LambdaQueryWrapper<>();
            trWrapper.eq(TreatmentRecord::getPatientId, task.getPatientId())
                    .eq(TreatmentRecord::getTherapistId, currentUserId)
                    .eq(TreatmentRecord::getTreatmentDate, today)
                    .eq(TreatmentRecord::getTreatmentItem, treatmentType);
            Long existingCount = treatmentRecordMapper.selectCount(trWrapper);
            if (existingCount == null || existingCount == 0) {
                stat.setPatientCount(stat.getPatientCount() + 1);
            }
            workloadStatMapper.updateById(stat);
        }

        // 4. Insert TreatmentRecord
        TreatmentRecord record = new TreatmentRecord();
        record.setPatientId(task.getPatientId());
        record.setTaskId(taskId);
        record.setTherapistId(currentUserId);
        record.setTreatmentDate(today);
        record.setTreatmentItem(task.getTreatmentItem());
        treatmentRecordMapper.insert(record);

        log.info("Task verified: id={}, verifierId={}", taskId, currentUserId);
    }

    // -------------------- revokeVerification --------------------

    @Transactional
    public void revokeVerification(Long taskId, String reason) {
        Long currentUserId = UserContext.getUserId();
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        if (!Objects.equals(task.getTherapistId(), currentUserId)) {
            throw new BusinessException("只有任务执行人才能撤销核销");
        }
        if (!VERIFIED.equals(task.getStatus())) {
            throw new BusinessException("只有已核销的任务才能撤销");
        }

        LocalDateTime now = LocalDateTime.now();

        // 1. Update task status to REVOKED
        task.setStatus(REVOKED);
        task.setRevokeTime(now);
        task.setRevokeReason(reason);
        taskMapper.updateById(task);

        // 2. Update TaskVerification record
        LambdaQueryWrapper<TaskVerification> verificationWrapper = new LambdaQueryWrapper<>();
        verificationWrapper.eq(TaskVerification::getTaskId, taskId)
                .eq(TaskVerification::getRevoked, 0)
                .orderByDesc(TaskVerification::getId)
                .last("LIMIT 1");
        TaskVerification verification = taskVerificationMapper.selectOne(verificationWrapper);
        if (verification != null) {
            verification.setRevoked(1);
            verification.setRevokeTime(now);
            verification.setRevokeReason(reason);
            taskVerificationMapper.updateById(verification);
        }

        // 3. Decrement WorkloadStat
        LocalDate today = LocalDate.now();
        String treatmentType = task.getTreatmentItem();

        LambdaQueryWrapper<WorkloadStat> statWrapper = new LambdaQueryWrapper<>();
        statWrapper.eq(WorkloadStat::getUserId, currentUserId)
                .eq(WorkloadStat::getStatDate, today)
                .eq(WorkloadStat::getTreatmentType, treatmentType);
        WorkloadStat stat = workloadStatMapper.selectOne(statWrapper);
        if (stat != null) {
            int newCount = Math.max(0, stat.getTreatmentCount() - 1);
            stat.setTreatmentCount(newCount);
            if (newCount == 0) {
                stat.setPatientCount(0);
            }
            workloadStatMapper.updateById(stat);
        }

        log.info("Task verification revoked: id={}, reason={}, userId={}", taskId, reason, currentUserId);
    }

    // -------------------- getCalendarTasks --------------------

    public List<Task> getCalendarTasks(Long therapistId, int year, int month) {
        Long currentUserId = UserContext.getUserId();

        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        if (therapistId == null) {
            therapistId = currentUserId;
        }

        // Non-admin users can only view their own calendar
        if (!UserContext.isAdmin() && !Objects.equals(therapistId, currentUserId)) {
            therapistId = currentUserId;
        }

        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Task::getTherapistId, therapistId)
                .ge(Task::getTaskDate, startDate)
                .le(Task::getTaskDate, endDate)
                .orderByAsc(Task::getTaskDate)
                .orderByAsc(Task::getTimeSlot);

        return taskMapper.selectList(wrapper);
    }

    // -------------------- generateTasksFromOrder --------------------

    @Transactional
    public List<Task> generateTasksFromOrder(Long orderId) {
        MedicalOrder order = medicalOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("医嘱不存在");
        }
        if (!"APPROVED".equals(order.getStatus())) {
            throw new BusinessException("只有已审核通过的医嘱才能生成任务");
        }

        // Find all schedule entries for this order
        LambdaQueryWrapper<PatientSchedule> scheduleWrapper = new LambdaQueryWrapper<>();
        scheduleWrapper.eq(PatientSchedule::getSourceId, orderId)
                .eq(PatientSchedule::getEventType, "ORDER")
                .eq(PatientSchedule::getStatus, "SCHEDULED");
        List<PatientSchedule> schedules = patientScheduleMapper.selectList(scheduleWrapper);

        if (schedules.isEmpty()) {
            throw new BusinessException("该医嘱暂无排程条目，请先审核通过以生成排程");
        }

        // Get the patient's attending therapist for group lookup
        User therapist = null;
        if (schedules.get(0).getTherapistId() != null) {
            therapist = userMapper.selectById(schedules.get(0).getTherapistId());
        }

        List<Task> createdTasks = new ArrayList<>();
        for (PatientSchedule schedule : schedules) {
            // Skip if task already exists for this order+date+timeslot
            if (taskMapper.existsByOrderAndDateAndSlot(orderId, schedule.getScheduleDate(), schedule.getTimeSlot())) {
                continue;
            }

            Task task = new Task();
            task.setPatientId(order.getPatientId());
            task.setOrderId(orderId);
            task.setTherapistId(schedule.getTherapistId());
            task.setGroupId(therapist != null ? therapist.getGroupId() : null);
            task.setTaskDate(schedule.getScheduleDate());
            task.setTimeSlot(schedule.getTimeSlot());
            task.setTreatmentItem(order.getTreatmentItem());
            task.setStatus(PENDING);

            taskMapper.insert(task);
            createdTasks.add(task);

            // Mark schedule entry as TASK_GENERATED
            schedule.setStatus("TASK_GENERATED");
            patientScheduleMapper.updateById(schedule);
        }

        log.info("Generated {} tasks from order id={}, skipped {} duplicates",
                createdTasks.size(), orderId, schedules.size() - createdTasks.size());
        return createdTasks;
    }

    // -------------------- generateTaskFromSchedule --------------------

    @Transactional
    public Task generateTaskFromSchedule(Long scheduleId) {
        PatientSchedule schedule = patientScheduleMapper.selectById(scheduleId);
        if (schedule == null) {
            throw new BusinessException("排程条目不存在");
        }
        if (!"ORDER".equals(schedule.getEventType())) {
            throw new BusinessException("该排程条目不是医嘱类型");
        }
        if (!"SCHEDULED".equals(schedule.getStatus())) {
            throw new BusinessException("该排程条目已生成过任务");
        }

        MedicalOrder order = medicalOrderMapper.selectById(schedule.getSourceId());
        if (order == null) {
            throw new BusinessException("关联医嘱不存在");
        }

        // Check for duplicate
        if (taskMapper.existsByOrderAndDateAndSlot(order.getId(), schedule.getScheduleDate(), schedule.getTimeSlot())) {
            throw new BusinessException("该时段已存在任务");
        }

        User therapist = userMapper.selectById(schedule.getTherapistId());

        Task task = new Task();
        task.setPatientId(order.getPatientId());
        task.setOrderId(order.getId());
        task.setTherapistId(schedule.getTherapistId());
        task.setGroupId(therapist != null ? therapist.getGroupId() : null);
        task.setTaskDate(schedule.getScheduleDate());
        task.setTimeSlot(schedule.getTimeSlot());
        task.setTreatmentItem(order.getTreatmentItem());
        task.setStatus(PENDING);

        taskMapper.insert(task);

        schedule.setStatus("TASK_GENERATED");
        patientScheduleMapper.updateById(schedule);

        log.info("Generated task id={} from schedule id={}", task.getId(), scheduleId);
        return task;
    }
}
