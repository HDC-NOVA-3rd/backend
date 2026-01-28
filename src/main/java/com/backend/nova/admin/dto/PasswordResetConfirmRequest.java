package com.backend.nova.admin.dto;

/**
 * 비밀번호 재설정 요청 DTO
 */
public record PasswordResetConfirmRequest(
        String loginId,
        String newPassword
) {}
