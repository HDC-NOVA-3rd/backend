package com.backend.nova.admin.dto;

public record OtpSetupResponse(
        String secret,      // 수동 입력용
        String qrCodeUrl    // QR 이미지 URL (base64 or otpauth)
) {}

