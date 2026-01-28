package com.backend.nova.admin.dto;

import lombok.Getter;

@Getter
public class AdminLoginOtpVerifyRequest {
    private String loginId;
    private String otpCode;
}

