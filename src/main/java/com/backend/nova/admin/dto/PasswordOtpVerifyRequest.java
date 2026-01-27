package com.backend.nova.admin.dto;

import lombok.Getter;

@Getter
public class PasswordOtpVerifyRequest {
    private String loginId;
    private String otp;
}
