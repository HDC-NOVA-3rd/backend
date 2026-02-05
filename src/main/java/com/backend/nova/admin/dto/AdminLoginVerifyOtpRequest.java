package com.backend.nova.admin.dto;

import lombok.Getter;

@Getter
public class AdminLoginVerifyOtpRequest {
    private String otp;
    private String challengeToken;
    private String deviceId;
}

