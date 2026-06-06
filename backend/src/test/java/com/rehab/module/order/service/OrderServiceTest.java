package com.rehab.module.order.service;

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
import com.rehab.module.websocket.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private MedicalOrderMapper medicalOrderMapper;
    @Mock private PatientScheduleMapper patientScheduleMapper;
    @Mock private PatientMapper patientMapper;
    @Mock private UserMapper userMapper;
    @Mock private NotificationService notificationService;
    @Mock private com.rehab.module.treatment.service.TreatmentService treatmentService;

    @InjectMocks
    private OrderService orderService;

    private MedicalOrder pendingOrder;
    private MockedStatic<UserContext> userContextMock;

    @BeforeEach
    void setUp() {
        pendingOrder = new MedicalOrder();
        pendingOrder.setId(1L);
        pendingOrder.setPatientId(10L);
        pendingOrder.setDoctorId(3L);
        pendingOrder.setTreatmentItem("推拿治疗");
        pendingOrder.setFrequency("每日1次");
        pendingOrder.setDailyCount(1);
        pendingOrder.setPeriodStart(LocalDate.of(2026, 6, 1));
        pendingOrder.setPeriodEnd(LocalDate.of(2026, 6, 7));
        pendingOrder.setStatus("PENDING_REVIEW");

        userContextMock = mockStatic(UserContext.class);
        userContextMock.when(UserContext::getUserId).thenReturn(3L);
        userContextMock.when(UserContext::isAdmin).thenReturn(false);
        userContextMock.when(UserContext::isDoctor).thenReturn(true);
        userContextMock.when(UserContext::getGroupId).thenReturn(5L);
    }

    @AfterEach
    void tearDown() {
        userContextMock.close();
    }

    @Test
    void createOrder_SetsDoctorAndAutoSubmits() {
        when(medicalOrderMapper.insert(any(MedicalOrder.class))).thenAnswer(inv -> {
            inv.getArgument(0, MedicalOrder.class).setId(99L);
            return 1;
        });

        MedicalOrder input = new MedicalOrder();
        input.setPatientId(10L);
        input.setTreatmentItem("测试");

        MedicalOrder result = orderService.createOrder(input);

        assertThat(result.getStatus()).isEqualTo("PENDING_REVIEW");
        verify(medicalOrderMapper).insert(any(MedicalOrder.class));
    }

    @Test
    void approveOrder_GeneratesScheduleEntries() {
        when(medicalOrderMapper.selectById(1L)).thenReturn(pendingOrder);

        Patient patient = new Patient();
        patient.setAttendingTherapistId(2L);
        when(patientMapper.selectById(10L)).thenReturn(patient);

        User therapist = new User();
        therapist.setId(2L);
        therapist.setGroupId(5L);
        when(userMapper.selectById(2L)).thenReturn(therapist);

        orderService.approveOrder(1L, "ok");

        assertThat(pendingOrder.getStatus()).isEqualTo("APPROVED");
        verify(patientScheduleMapper, times(7)).insert(any(PatientSchedule.class));
    }

    @Test
    void approveOrder_WhenNotPendingReview_ThrowsException() {
        pendingOrder.setStatus("APPROVED");
        when(medicalOrderMapper.selectById(1L)).thenReturn(pendingOrder);

        assertThatThrownBy(() -> orderService.approveOrder(1L, ""))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("待审核");
    }
}
