package com.backend.nova.admin.dto;

public record AdminLoginResponse(
        Long adminId,
        String name,
        String accessToken
) {
    public AdminLoginResponse {
        if (adminId == null) {
            throw new IllegalArgumentException("adminId는 필수입니다.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name은 필수입니다.");
        }
    }
}
