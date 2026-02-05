package com.backend.nova.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminLoginRequest(
        String loginId,
        String password,
        String otpCode
) {}
