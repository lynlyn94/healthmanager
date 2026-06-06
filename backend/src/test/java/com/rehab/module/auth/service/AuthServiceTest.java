package com.rehab.module.auth.service;

import com.rehab.common.exception.BusinessException;
import com.rehab.module.auth.dto.LoginRequest;
import com.rehab.module.auth.dto.LoginResponse;
import com.rehab.module.auth.entity.User;
import com.rehab.module.auth.mapper.UserMapper;
import com.rehab.infrastructure.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserMapper userMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtils jwtUtils;

    @InjectMocks
    private AuthService authService;

    private User activeUser;

    @BeforeEach
    void setUp() {
        activeUser = new User();
        activeUser.setId(1L);
        activeUser.setUsername("therapist1");
        activeUser.setPassword("$2a$10$encodedPassword");
        activeUser.setRealName("张治疗师");
        activeUser.setRole("THERAPIST");
        activeUser.setStatus(1);
    }

    @Test
    void login_WithCorrectCredentials_ReturnsToken() {
        when(userMapper.selectOne(any())).thenReturn(activeUser);
        when(passwordEncoder.matches("rehab123", "$2a$10$encodedPassword")).thenReturn(true);
        when(jwtUtils.generateToken(1L, "therapist1", "THERAPIST")).thenReturn("test-jwt-token");

        LoginRequest req = new LoginRequest();
        req.setUsername("therapist1");
        req.setPassword("rehab123");

        LoginResponse resp = authService.login(req);

        assertThat(resp).isNotNull();
        assertThat(resp.getToken()).isEqualTo("test-jwt-token");
        assertThat(resp.getUserId()).isEqualTo(1L);
    }

    @Test
    void login_WithWrongPassword_ThrowsException() {
        when(userMapper.selectOne(any())).thenReturn(activeUser);
        when(passwordEncoder.matches("wrong-password", "$2a$10$encodedPassword")).thenReturn(false);

        LoginRequest req = new LoginRequest();
        req.setUsername("therapist1");
        req.setPassword("wrong-password");

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void login_WhenUserNotFound_ThrowsException() {
        when(userMapper.selectOne(any())).thenReturn(null);

        LoginRequest req = new LoginRequest();
        req.setUsername("unknown");
        req.setPassword("rehab123");

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BusinessException.class);
    }
}
