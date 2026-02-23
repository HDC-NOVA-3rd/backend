package com.backend.nova.admin.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 관리자 비밀번호 변경요청
 */
public record AdminPasswordChangeRequest(
        @NotBlank String currentPassword
) {}
