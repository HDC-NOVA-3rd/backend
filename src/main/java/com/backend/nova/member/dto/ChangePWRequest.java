package com.backend.nova.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ChangePWRequest(
        @Schema(description = "현재 비밀번호 (검증용)", example = "tempPassword123")
        String currentPassword,

        @Schema(description = "새로운 비밀번호", example = "newStrongPassword!@#")
        String newPassword
) {
}
