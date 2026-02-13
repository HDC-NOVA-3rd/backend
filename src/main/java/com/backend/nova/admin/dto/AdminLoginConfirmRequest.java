package com.backend.nova.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminLoginConfirmRequest(
        @NotBlank(message = "loginId는 필수입니다.")
        String loginId,
        @NotBlank(message = "otpCode는 필수입니다.")
        String otpCode
) {
}
