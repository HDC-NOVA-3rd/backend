package com.backend.nova.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminLoginResponse {
    private Long adminId;
    private String name;
    private String accessToken; // JWT 사용 시
}
