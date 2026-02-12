package com.backend.nova.admin.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 비밀번호 초기화 요청
 */
public record AdminPasswordResetRequest(
        @NotBlank String loginId,
        @NotBlank String email
) {}
