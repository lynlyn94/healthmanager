package com.rehab.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rehab.common.PageResult;
import com.rehab.common.exception.BusinessException;
import com.rehab.module.auth.entity.User;
import com.rehab.module.auth.mapper.UserMapper;
import com.rehab.module.patient.entity.Patient;
import com.rehab.module.patient.mapper.PatientMapper;
import com.rehab.module.system.entity.Notification;
import com.rehab.module.system.entity.OperationLog;
import com.rehab.module.system.entity.SystemDict;
import com.rehab.module.system.entity.TherapyGroup;
import com.rehab.module.system.mapper.NotificationMapper;
import com.rehab.module.system.mapper.OperationLogMapper;
import com.rehab.module.system.mapper.SystemDictMapper;
import com.rehab.module.system.mapper.TherapyGroupMapper;
import com.rehab.module.task.entity.Task;
import com.rehab.module.task.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemService {

    private final UserMapper userMapper;
    private final TherapyGroupMapper therapyGroupMapper;
    private final SystemDictMapper systemDictMapper;
    private final OperationLogMapper operationLogMapper;
    private final PatientMapper patientMapper;
    private final TaskMapper taskMapper;
    private final NotificationMapper notificationMapper;
    private final PasswordEncoder passwordEncoder;

    // ================================================================
    // User management
    // ================================================================

    public PageResult<User> listUsers(int page, int size, String keyword, String role) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(User::getUsername, keyword)
                    .or()
                    .like(User::getRealName, keyword)
                    .or()
                    .like(User::getPhone, keyword));
        }
        if (StringUtils.hasText(role)) {
            wrapper.eq(User::getRole, role);
        }
        wrapper.orderByAsc(User::getId);

        Page<User> userPage = new Page<>(page, size);
        IPage<User> result = userMapper.selectPage(userPage, wrapper);
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    public User createUser(User user) {
        if (userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, user.getUsername())) > 0) {
            throw new BusinessException("用户名已存在");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        userMapper.insert(user);
        log.info("Created user: {}", user.getUsername());
        return user;
    }

    public User updateUser(User user) {
        User existing = userMapper.selectById(user.getId());
        if (existing == null) {
            throw new BusinessException("用户不存在");
        }
        if (StringUtils.hasText(user.getUsername()) && !user.getUsername().equals(existing.getUsername())) {
            if (userMapper.selectCount(
                    new LambdaQueryWrapper<User>().eq(User::getUsername, user.getUsername())) > 0) {
                throw new BusinessException("用户名已存在");
            }
        }
        // Prevent password field from being cleared on general update
        user.setPassword(null);
        userMapper.updateById(user);
        log.info("Updated user: {}", user.getId());
        return userMapper.selectById(user.getId());
    }

    public void deleteUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setStatus(0);
        userMapper.updateById(user);
        log.info("Disabled user: {}", id);
    }

    public void resetPassword(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setPassword(passwordEncoder.encode("rehab123"));
        userMapper.updateById(user);
        log.info("Reset password for user: {}", id);
    }

    // ================================================================
    // Therapy group management
    // ================================================================

    public List<TherapyGroup> listGroups() {
        return therapyGroupMapper.selectList(
                new LambdaQueryWrapper<TherapyGroup>().orderByAsc(TherapyGroup::getId));
    }

    public TherapyGroup createGroup(TherapyGroup group) {
        therapyGroupMapper.insert(group);
        log.info("Created group: {}", group.getGroupName());
        return group;
    }

    public TherapyGroup updateGroup(TherapyGroup group) {
        TherapyGroup existing = therapyGroupMapper.selectById(group.getId());
        if (existing == null) {
            throw new BusinessException("治疗组不存在");
        }
        therapyGroupMapper.updateById(group);
        log.info("Updated group: {}", group.getId());
        return therapyGroupMapper.selectById(group.getId());
    }

    public void deleteGroup(Long id) {
        if (userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getGroupId, id)) > 0) {
            throw new BusinessException("治疗组下存在用户，无法删除");
        }
        therapyGroupMapper.deleteById(id);
        log.info("Deleted group: {}", id);
    }

    // ================================================================
    // Dict management
    // ================================================================

    @Cacheable(value = "dict", key = "#dictType")
    public List<SystemDict> listByType(String dictType) {
        return systemDictMapper.selectList(
                new LambdaQueryWrapper<SystemDict>()
                        .eq(SystemDict::getDictType, dictType)
                        .orderByAsc(SystemDict::getSortOrder));
    }

    @Cacheable(value = "dict", key = "'allTypes'")
    public List<String> listAllTypes() {
        List<SystemDict> all = systemDictMapper.selectList(new LambdaQueryWrapper<>());
        return all.stream()
                .map(SystemDict::getDictType)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    @CacheEvict(value = "dict", allEntries = true)
    public SystemDict createDict(SystemDict dict) {
        systemDictMapper.insert(dict);
        log.info("Created dict: {} - {}", dict.getDictType(), dict.getDictCode());
        return dict;
    }

    @CacheEvict(value = "dict", allEntries = true)
    public SystemDict updateDict(SystemDict dict) {
        SystemDict existing = systemDictMapper.selectById(dict.getId());
        if (existing == null) {
            throw new BusinessException("字典项不存在");
        }
        systemDictMapper.updateById(dict);
        log.info("Updated dict: {}", dict.getId());
        return systemDictMapper.selectById(dict.getId());
    }

    @CacheEvict(value = "dict", allEntries = true)
    public void deleteDict(Long id) {
        systemDictMapper.deleteById(id);
        log.info("Deleted dict: {}", id);
    }

    // ================================================================
    // Operation log
    // ================================================================

    public PageResult<OperationLog> listLogs(int page, int size, Long userId, String action,
                                              LocalDateTime startDate, LocalDateTime endDate) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(OperationLog::getUserId, userId);
        }
        if (StringUtils.hasText(action)) {
            wrapper.eq(OperationLog::getAction, action);
        }
        if (startDate != null) {
            wrapper.ge(OperationLog::getCreateTime, startDate);
        }
        if (endDate != null) {
            wrapper.le(OperationLog::getCreateTime, endDate);
        }
        wrapper.orderByDesc(OperationLog::getId);

        Page<OperationLog> logPage = new Page<>(page, size);
        IPage<OperationLog> result = operationLogMapper.selectPage(logPage, wrapper);
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    public OperationLog getLogById(Long id) {
        OperationLog log = operationLogMapper.selectById(id);
        if (log == null) {
            throw new BusinessException("日志不存在");
        }
        return log;
    }

    // ================================================================
    // System overview stats
    // ================================================================

    public Map<String, Object> getSystemStats() {
        Map<String, Object> stats = new HashMap<>();
        LocalDate today = LocalDate.now();

        // totalUsers: count from user table (is_deleted=0, handled by @TableLogic)
        Long totalUsers = userMapper.selectCount(null);
        stats.put("totalUsers", totalUsers);

        // activePatients: count patients where status='IN_HOSPITAL'
        Long activePatients = patientMapper.selectCount(
                new LambdaQueryWrapper<Patient>().eq(Patient::getStatus, "IN_HOSPITAL"));
        stats.put("activePatients", activePatients);

        // todayTasks: tasks where task_date=today and status in (PENDING, IN_PROGRESS, VERIFIED)
        Long todayTasks = taskMapper.selectCount(
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getTaskDate, today)
                        .in(Task::getStatus, "PENDING", "IN_PROGRESS", "VERIFIED"));
        stats.put("todayTasks", todayTasks);

        // monthlyVerifications: tasks verified in current month
        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime monthEnd = today.plusMonths(1).withDayOfMonth(1).atStartOfDay();
        Long monthlyVerifications = taskMapper.selectCount(
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getStatus, "VERIFIED")
                        .ge(Task::getVerificationTime, monthStart)
                        .lt(Task::getVerificationTime, monthEnd));
        stats.put("monthlyVerifications", monthlyVerifications);

        // groupsCount: count from therapy_group
        Long groupsCount = therapyGroupMapper.selectCount(null);
        stats.put("groupsCount", groupsCount);

        // todayVerified: tasks verified today
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.plusDays(1).atStartOfDay();
        Long todayVerified = taskMapper.selectCount(
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getStatus, "VERIFIED")
                        .ge(Task::getVerificationTime, todayStart)
                        .lt(Task::getVerificationTime, todayEnd));
        stats.put("todayVerified", todayVerified);

        // byRole: users grouped by role
        List<User> allUsers = userMapper.selectList(null);
        Map<String, Long> byRole = allUsers.stream()
                .collect(Collectors.groupingBy(
                        u -> u.getRole() != null ? u.getRole() : "未知",
                        Collectors.counting()));
        stats.put("byRole", byRole);

        // byStatus: patients grouped by status
        List<Patient> allPatients = patientMapper.selectList(null);
        Map<String, Long> byStatus = allPatients.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getStatus() != null ? p.getStatus() : "未知",
                        Collectors.counting()));
        stats.put("byStatus", byStatus);

        return stats;
    }

    /**
     * Query verified tasks within date range, group by the therapist's role.
     * Returns list of {role: "治疗师", count: 42}
     */
    public List<Map<String, Object>> getWorkloadByRole(String startDate, String endDate) {
        LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
        LocalDateTime end = LocalDate.parse(endDate).plusDays(1).atStartOfDay();

        List<Task> tasks = taskMapper.selectList(
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getStatus, "VERIFIED")
                        .ge(Task::getVerificationTime, start)
                        .lt(Task::getVerificationTime, end));

        // Collect distinct therapist IDs
        Set<Long> therapistIds = tasks.stream()
                .map(Task::getTherapistId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Build role lookup map
        final Map<Long, String> userRoleMap;
        if (!therapistIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(therapistIds);
            userRoleMap = users.stream()
                    .collect(Collectors.toMap(User::getId,
                            u -> u.getRole() != null ? u.getRole() : "未知"));
        } else {
            userRoleMap = new HashMap<>();
        }

        // Group task count by therapist role
        Map<String, Long> roleCount = tasks.stream()
                .collect(Collectors.groupingBy(
                        t -> userRoleMap.getOrDefault(t.getTherapistId(), "未知"),
                        Collectors.counting()));

        return roleCount.entrySet().stream()
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("role", e.getKey());
                    m.put("count", e.getValue());
                    return m;
                })
                .collect(Collectors.toList());
    }

    /**
     * Query verified tasks within date range, group by treatment type.
     * Returns list of {type: "PT", count: 30}
     */
    public List<Map<String, Object>> getTreatmentTypeDistribution(String startDate, String endDate) {
        LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
        LocalDateTime end = LocalDate.parse(endDate).plusDays(1).atStartOfDay();

        List<Task> tasks = taskMapper.selectList(
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getStatus, "VERIFIED")
                        .ge(Task::getVerificationTime, start)
                        .lt(Task::getVerificationTime, end));

        Map<String, Long> typeCount = tasks.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getTreatmentItem() != null ? t.getTreatmentItem() : "未知",
                        Collectors.counting()));

        return typeCount.entrySet().stream()
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("type", e.getKey());
                    m.put("count", e.getValue());
                    return m;
                })
                .collect(Collectors.toList());
    }

    /**
     * Daily verification trend within date range.
     * Returns list of {date: "2026-06-01", count: 15} for each day.
     */
    public List<Map<String, Object>> getDailyVerificationTrend(String startDate, String endDate) {
        LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
        LocalDateTime end = LocalDate.parse(endDate).plusDays(1).atStartOfDay();

        List<Task> tasks = taskMapper.selectList(
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getStatus, "VERIFIED")
                        .ge(Task::getVerificationTime, start)
                        .lt(Task::getVerificationTime, end));

        // Group by date
        Map<LocalDate, Long> dailyCount = tasks.stream()
                .filter(t -> t.getVerificationTime() != null)
                .collect(Collectors.groupingBy(
                        t -> t.getVerificationTime().toLocalDate(),
                        Collectors.counting()));

        // Fill all days in range (zero for days with no data)
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate current = LocalDate.parse(startDate);
        LocalDate endExclusive = LocalDate.parse(endDate).plusDays(1);
        while (current.isBefore(endExclusive)) {
            Map<String, Object> m = new HashMap<>();
            m.put("date", current.toString());
            m.put("count", dailyCount.getOrDefault(current, 0L));
            result.add(m);
            current = current.plusDays(1);
        }
        return result;
    }

    // ================================================================
    // Notification management
    // ================================================================

    public Notification createNotification(Long userId, String title, String content, String type, Long sourceId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type);
        notification.setIsRead(0);
        notification.setSourceId(sourceId);
        notificationMapper.insert(notification);
        log.info("Created notification: id={}, userId={}, type={}", notification.getId(), userId, type);
        return notification;
    }

    public PageResult<Notification> listNotifications(Long userId, int page, int size) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(Notification::getUserId, userId);
        }
        wrapper.orderByDesc(Notification::getCreateTime);

        Page<Notification> mpPage = new Page<>(page, size);
        IPage<Notification> result = notificationMapper.selectPage(mpPage, wrapper);
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    public void markAsRead(Long notificationId) {
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification == null) {
            throw new BusinessException("通知不存在");
        }
        notification.setIsRead(1);
        notificationMapper.updateById(notification);
        log.info("Marked notification {} as read", notificationId);
    }

    public void markAllAsRead(Long userId) {
        List<Notification> unreadList = notificationMapper.selectList(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .eq(Notification::getIsRead, 0));
        for (Notification n : unreadList) {
            n.setIsRead(1);
            notificationMapper.updateById(n);
        }
        log.info("Marked all {} unread notifications as read for user {}", unreadList.size(), userId);
    }

    public long getUnreadCount(Long userId) {
        return notificationMapper.selectCount(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .eq(Notification::getIsRead, 0));
    }
}
