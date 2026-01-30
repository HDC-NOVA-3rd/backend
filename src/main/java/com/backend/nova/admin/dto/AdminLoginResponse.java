package com.backend.nova.admin.dto;

public record AdminLoginResponse(
        Long adminId,
        String name,
        String accessToken,
        String refreshToken
) {
    public AdminLoginResponse {
        if (adminId == null) {
            throw new IllegalArgumentException("adminId는 필수입니다.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name은 필수입니다.");
        }
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("accessToken은 필수입니다.");
        }
        // refreshToken은 선택 사항이므로 null 허용
    }
}
