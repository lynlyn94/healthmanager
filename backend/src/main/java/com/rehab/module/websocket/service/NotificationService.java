package com.rehab.module.websocket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rehab.module.auth.entity.User;
import com.rehab.module.auth.mapper.UserMapper;
import com.rehab.module.patient.entity.Patient;
import com.rehab.module.patient.mapper.PatientMapper;
import com.rehab.module.system.service.SystemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final SystemService systemService;
    private final UserMapper userMapper;
    private final PatientMapper patientMapper;

    /**
     * Send a task-assignment notification to a specific user.
     */
    public void notifyTaskAssigned(Long userId, String message) {
        log.info("Notify task assigned: userId={}, message={}", userId, message);
        messagingTemplate.convertAndSend("/topic/task/" + userId, message);
        systemService.createNotification(userId, "任务分配", message, "TASK", null);
    }

    /**
     * Send a group task notification to all members of a group.
     */
    public void notifyGroupTask(Long groupId, String message) {
        log.info("Notify group task: groupId={}, message={}", groupId, message);
        messagingTemplate.convertAndSend("/topic/task/group/" + groupId, message);

        List<User> groupUsers = userMapper.selectList(
                new LambdaQueryWrapper<User>().eq(User::getGroupId, groupId));
        for (User user : groupUsers) {
            systemService.createNotification(user.getId(), "小组任务", message, "TASK", null);
        }
    }

    /**
     * Notify that a medical order has changed for a patient.
     */
    public void notifyOrderChanged(Long patientId, String message) {
        log.info("Notify order changed: patientId={}, message={}", patientId, message);
        messagingTemplate.convertAndSend("/topic/order/" + patientId, message);

        Patient patient = patientMapper.selectById(patientId);
        if (patient != null && patient.getAttendingTherapistId() != null) {
            systemService.createNotification(
                    patient.getAttendingTherapistId(), "医嘱变更", message, "ORDER", patientId);
        }
    }

    /**
     * Broadcast a system-wide announcement.
     */
    public void notifySystemAnnouncement(String message) {
        log.info("Notify system announcement: {}", message);
        messagingTemplate.convertAndSend("/topic/system/announcement", message);
    }
}
