package com.backend.nova.admin.dto;

/**
 * 관리자 비밀번호 OTP 검증 요청 DTO
 */
public record PasswordOtpVerifyRequest(
        String loginId,
        String otp
) {}
