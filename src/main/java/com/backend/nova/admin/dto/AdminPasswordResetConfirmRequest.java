package com.backend.nova.admin.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 비밀번호 초기화
 */
public record AdminPasswordResetConfirmRequest(
        @NotBlank String loginId,
        @NotBlank String otpCode,
        @NotBlank String newPassword,
        @NotBlank String passwordConfirm
) {}
