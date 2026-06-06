package com.rehab.module.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private Long userId;
    private String username;
    private String realName;
    private String role;
    private Long groupId;
    private String phone;
    private String email;
    private String avatar;
}
