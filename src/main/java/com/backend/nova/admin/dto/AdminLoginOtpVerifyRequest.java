package com.backend.nova.admin.dto;

public record AdminLoginOtpVerifyRequest(
        String loginId,
        String otpCode
) {
    public AdminLoginOtpVerifyRequest {
        if (loginId == null || loginId.isBlank()) {
            throw new IllegalArgumentException("loginId는 필수입니다.");
        }
        if (otpCode == null || otpCode.isBlank()) {
            throw new IllegalArgumentException("otpCode는 필수입니다.");
        }
    }
}
