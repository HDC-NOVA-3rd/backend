package com.backend.nova.admin.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 관리자 비밀번호 변경 수락
 */
public record AdminPasswordChangeConfirmRequest(
        @NotBlank String otpCode,
        @NotBlank String currentPassword,
        @NotBlank String newPassword,
        @NotBlank String passwordConfirm
) {}
