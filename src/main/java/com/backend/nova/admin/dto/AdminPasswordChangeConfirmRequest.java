package com.backend.nova.admin.dto;

/**
 * 관리자 비밀번호 변경 요청 DTO
 */
public record AdminPasswordChangeConfirmRequest(
        String currentPassword,
        String newPassword
) {}
