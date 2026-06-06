package com.rehab.module.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rehab.common.PageResult;
import com.rehab.common.exception.BusinessException;
import com.rehab.infrastructure.UserContext;
import com.rehab.module.auth.entity.User;
import com.rehab.module.auth.mapper.UserMapper;
import com.rehab.module.order.entity.MedicalOrder;
import com.rehab.module.order.mapper.MedicalOrderMapper;
import com.rehab.module.patient.entity.Patient;
import com.rehab.module.patient.mapper.PatientMapper;
import com.rehab.module.schedule.entity.PatientSchedule;
import com.rehab.module.schedule.mapper.PatientScheduleMapper;
import com.rehab.module.task.entity.Task;
import com.rehab.module.task.mapper.TaskMapper;
import com.rehab.module.treatment.service.TreatmentService;
import com.rehab.module.websocket.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final MedicalOrderMapper medicalOrderMapper;
    private final PatientScheduleMapper patientScheduleMapper;
    private final PatientMapper patientMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;
    private final TreatmentService treatmentService;
    private final TaskMapper taskMapper;
    private final ObjectMapper objectMapper;

    // ---- helpers ----

    private void checkDoctorOrAdmin() {
        if (!UserContext.isAdmin() && !UserContext.isDoctor()) {
            throw new BusinessException(403, "仅医生和管理员可以操作医嘱");
        }
    }

    private MedicalOrder requireOrder(Long id) {
        MedicalOrder order = medicalOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(404, "医嘱不存在");
        }
        return order;
    }

    /**
     * Map dailyCount to time-slot labels.
     */
    private List<String> getTimeSlots(int dailyCount) {
        List<String> slots = new ArrayList<>();
        if (dailyCount >= 1) slots.add("09:00-10:00");
        if (dailyCount >= 2) slots.add("14:00-15:00");
        if (dailyCount >= 3) slots.add("16:00-17:00");
        while (slots.size() < dailyCount) {
            slots.add("时段" + (slots.size() + 1));
        }
        return slots;
    }

    /**
     * Determine which dates need schedule entries based on the order's
     * frequency and period.
     */
    private List<LocalDate> getScheduleDates(MedicalOrder order) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate start = order.getPeriodStart();
        LocalDate end = order.getPeriodEnd();
        if (start == null || end == null || start.isAfter(end)) {
            return dates;
        }

        String freq = order.getFrequency();
        if (freq == null) freq = "";

        if (freq.startsWith("每日")) {
            // every day in the period
            LocalDate cursor = start;
            while (!cursor.isAfter(end)) {
                dates.add(cursor);
                cursor = cursor.plusDays(1);
            }
        } else if (freq.startsWith("每周")) {
            // extract count number from string like "每周2次"
            int weeklyCount = 1;
            try {
                String numStr = freq.replaceAll("[^0-9]", "");
                if (!numStr.isEmpty()) {
                    weeklyCount = Integer.parseInt(numStr);
                }
            } catch (NumberFormatException ignored) {
            }
            weeklyCount = Math.max(1, Math.min(weeklyCount, 7));
            // Distribute evenly: pick weeklyCount weekdays, starting Monday.
            int[] offsets = new int[weeklyCount];
            for (int i = 0; i < weeklyCount; i++) {
                offsets[i] = i * 7 / weeklyCount;
            }
            LocalDate cursor = start;
            while (!cursor.isAfter(end)) {
                int dow = cursor.getDayOfWeek().getValue() - 1; // Mon=0
                for (int off : offsets) {
                    if (dow == off) {
                        dates.add(cursor);
                        break;
                    }
                }
                cursor = cursor.plusDays(1);
            }
        } else {
            // fallback: every day
            LocalDate cursor = start;
            while (!cursor.isAfter(end)) {
                dates.add(cursor);
                cursor = cursor.plusDays(1);
            }
        }
        return dates;
    }

    /**
     * Build PatientSchedule entries for an approved order.
     */
    private List<PatientSchedule> generateScheduleEntries(MedicalOrder order) {
        List<PatientSchedule> schedules = new ArrayList<>();
        List<LocalDate> dates = getScheduleDates(order);
        List<String> timeSlots = getTimeSlots(order.getDailyCount() != null ? order.getDailyCount() : 1);

        // Get the patient to know attending therapist
        Patient patient = patientMapper.selectById(order.getPatientId());

        for (LocalDate date : dates) {
            for (String slot : timeSlots) {
                PatientSchedule ps = new PatientSchedule();
                ps.setPatientId(order.getPatientId());
                ps.setScheduleDate(date);
                ps.setTimeSlot(slot);
                ps.setEventType("ORDER");
                ps.setSourceId(order.getId());
                ps.setTitle(order.getTreatmentItem());
                ps.setDescription(order.getNote());
                ps.setStatus("SCHEDULED");
                if (patient != null) {
                    ps.setTherapistId(patient.getAttendingTherapistId());
                }
                schedules.add(ps);
            }
        }
        return schedules;
    }

    // ---- public API ----

    /**
     * Paginated query with optional filters.
     */
    public PageResult<MedicalOrder> listOrders(Long patientId, String status, int page, int size) {
        Page<MedicalOrder> mpPage = new Page<>(page, size);
        LambdaQueryWrapper<MedicalOrder> wrapper = new LambdaQueryWrapper<>();
        if (patientId != null) {
            wrapper.eq(MedicalOrder::getPatientId, patientId);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(MedicalOrder::getStatus, status);
        }
        wrapper.orderByDesc(MedicalOrder::getCreateTime);
        medicalOrderMapper.selectPage(mpPage, wrapper);

        // Populate patient and therapist names
        for (MedicalOrder order : mpPage.getRecords()) {
            Patient patient = patientMapper.selectById(order.getPatientId());
            if (patient != null) {
                order.setPatientName(patient.getName());
                if (patient.getAttendingTherapistId() != null) {
                    User therapist = userMapper.selectById(patient.getAttendingTherapistId());
                    if (therapist != null) {
                        order.setTherapistName(therapist.getRealName());
                    }
                }
            }
        }
        return PageResult.of(mpPage.getTotal(), page, size, mpPage.getRecords());
    }

    /**
     * Get a single order by id.
     */
    public MedicalOrder getById(Long id) {
        return requireOrder(id);
    }

    /**
     * Create an order: set doctor = current user, status = DRAFT,
     * then immediately auto-submit to PENDING_REVIEW.
     */
    @Transactional
    public MedicalOrder createOrder(MedicalOrder order) {
        checkDoctorOrAdmin();

        order.setDoctorId(UserContext.getUserId());
        order.setStatus("DRAFT");
        medicalOrderMapper.insert(order);

        // auto-submit
        order.setStatus("PENDING_REVIEW");
        medicalOrderMapper.updateById(order);

        // If order is linked to a treatment plan, mark plan as ORDERED
        if (order.getPlanId() != null) {
            treatmentService.markPlanOrdered(order.getPlanId());
        }

        log.info("Order created and submitted: id={}, patientId={}, doctorId={}",
                order.getId(), order.getPatientId(), order.getDoctorId());
        return order;
    }

    /**
     * Update an order.  Only DRAFT orders can be edited.
     */
    @Transactional
    public void updateOrder(MedicalOrder input) {
        checkDoctorOrAdmin();

        MedicalOrder existing = requireOrder(input.getId());
        if (!"DRAFT".equals(existing.getStatus())) {
            throw new BusinessException("仅草稿状态的医嘱可以编辑");
        }

        // Keep immutable fields
        input.setDoctorId(existing.getDoctorId());
        input.setStatus(existing.getStatus());

        medicalOrderMapper.updateById(input);
        log.info("Order updated: id={}", input.getId());
    }

    /**
     * Approve an order: set APPROVED, generate schedule entries, and
     * notify the therapist group via WebSocket.
     */
    @Transactional
    public void approveOrder(Long orderId, String comment) {
        checkDoctorOrAdmin();

        MedicalOrder order = requireOrder(orderId);
        if (!"PENDING_REVIEW".equals(order.getStatus())) {
            throw new BusinessException("仅待审核状态的医嘱可以审批");
        }

        order.setStatus("APPROVED");
        order.setReviewComment(comment);
        medicalOrderMapper.updateById(order);

        // Generate schedule entries
        List<PatientSchedule> schedules = generateScheduleEntries(order);
        for (PatientSchedule ps : schedules) {
            patientScheduleMapper.insert(ps);
        }
        log.info("Approved order id={}, created {} schedule entries", orderId, schedules.size());

        // WebSocket + DB notification to therapist group
        try {
            Patient patient = patientMapper.selectById(order.getPatientId());
            if (patient != null && patient.getAttendingTherapistId() != null) {
                User therapist = userMapper.selectById(patient.getAttendingTherapistId());
                if (therapist != null && therapist.getGroupId() != null) {
                    Map<String, Object> msg = new HashMap<>();
                    msg.put("type", "ORDER_APPROVED");
                    msg.put("orderId", order.getId());
                    msg.put("patientId", order.getPatientId());
                    msg.put("treatmentItem", order.getTreatmentItem());
                    msg.put("periodStart", order.getPeriodStart() != null ? order.getPeriodStart().toString() : null);
                    msg.put("periodEnd", order.getPeriodEnd() != null ? order.getPeriodEnd().toString() : null);
                    msg.put("scheduleCount", schedules.size());
                    String jsonMsg = objectMapper.writeValueAsString(msg);
                    notificationService.notifyGroupTask(therapist.getGroupId(), jsonMsg);
                    log.info("Notification sent to group {} for order id={}", therapist.getGroupId(), orderId);
                }
            }
        } catch (Exception e) {
            log.error("Notification failed for order id={}: {}", orderId, e.getMessage());
        }
    }

    /**
     * Reject an order with a reason.
     */
    @Transactional
    public void rejectOrder(Long orderId, String reason) {
        checkDoctorOrAdmin();

        MedicalOrder order = requireOrder(orderId);
        if (!"PENDING_REVIEW".equals(order.getStatus())) {
            throw new BusinessException("仅待审核状态的医嘱可以驳回");
        }

        order.setStatus("REJECTED");
        order.setReviewComment(reason);
        medicalOrderMapper.updateById(order);
        log.info("Order rejected: id={}, reason={}", orderId, reason);
    }

    /**
     * Cancel an order.
     */
    @Transactional
    public void cancelOrder(Long orderId) {
        checkDoctorOrAdmin();

        MedicalOrder order = requireOrder(orderId);
        if ("APPROVED".equals(order.getStatus()) || "CANCELLED".equals(order.getStatus())) {
            throw new BusinessException("已审批或已取消的医嘱不能取消");
        }

        order.setStatus("CANCELLED");
        medicalOrderMapper.updateById(order);
        log.info("Order cancelled: id={}", orderId);
    }

    @Transactional
    public void revokeApproval(Long orderId) {
        checkDoctorOrAdmin();
        MedicalOrder order = requireOrder(orderId);
        if (!"APPROVED".equals(order.getStatus())) {
            throw new BusinessException("只有已审核通过的医嘱才能撤销");
        }
        // Check if tasks have already been generated (scheduled)
        Long taskCount = taskMapper.selectCount(
                new LambdaQueryWrapper<Task>().eq(Task::getOrderId, orderId));
        if (taskCount != null && taskCount > 0) {
            throw new BusinessException("该医嘱已排程生成任务，无法撤销审核");
        }
        int count = (order.getRevokeCount() != null ? order.getRevokeCount() : 0) + 1;
        order.setRevokeCount(count);
        if (count >= 3) {
            order.setStatus("CANCELLED");
            order.setReviewComment("撤销审核超过3次，自动作废");
            log.info("Order auto-cancelled after {} revocations: id={}", count, orderId);
        } else {
            order.setStatus("DRAFT");
            log.info("Order approval revoked ({}): id={}", count, orderId);
        }
        medicalOrderMapper.updateById(order);
    }

    @Transactional
    public void approveOrderDirect(Long orderId) {
        checkDoctorOrAdmin();
        MedicalOrder order = requireOrder(orderId);
        if (!"DRAFT".equals(order.getStatus())) {
            throw new BusinessException("只有草稿状态的医嘱可以直接审核通过");
        }
        order.setStatus("APPROVED");
        medicalOrderMapper.updateById(order);
        log.info("Order directly approved: id={}", orderId);
    }
}
