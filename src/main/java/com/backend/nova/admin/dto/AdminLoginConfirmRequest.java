package com.backend.nova.admin.dto;

public record AdminLoginConfirmRequest(
        String loginId,
        String otpCode
) {
}
