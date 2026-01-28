package com.backend.nova.admin.dto;

public record AdminLoginRequest(
        String loginId,
        String password
) {
    public AdminLoginRequest {
        if (loginId == null || loginId.isBlank()) {
            throw new IllegalArgumentException("loginId는 필수입니다.");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("password는 필수입니다.");
        }
    }
}
