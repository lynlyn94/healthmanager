package com.rehab.module.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rehab.common.exception.BusinessException;
import com.rehab.infrastructure.JwtUtils;
import com.rehab.module.auth.dto.LoginRequest;
import com.rehab.module.auth.dto.LoginResponse;
import com.rehab.module.auth.entity.User;
import com.rehab.module.auth.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()));
        if (user == null) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        if (user.getStatus() == 0) {
            throw new BusinessException(403, "账号已被禁用");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }

        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole());

        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .role(user.getRole())
                .groupId(user.getGroupId())
                .phone(user.getPhone())
                .email(user.getEmail())
                .avatar(user.getAvatar())
                .build();
    }

    public User getUserInfo(Long userId) {
        return userMapper.selectById(userId);
    }

    public void updateUserInfo(Long userId, String realName, String phone, String email, String avatar) {
        User user = userMapper.selectById(userId);
        if (realName != null) user.setRealName(realName);
        if (phone != null) user.setPhone(phone);
        if (email != null) user.setEmail(email);
        if (avatar != null) user.setAvatar(avatar);
        userMapper.updateById(user);
    }

    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }
}
