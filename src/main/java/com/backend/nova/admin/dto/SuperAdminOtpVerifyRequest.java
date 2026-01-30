package com.backend.nova.admin.dto;

/**
 * 슈퍼 관리자 OTP 검증 요청 DTO
 */
public record SuperAdminOtpVerifyRequest(
        /**
         * 관리자 로그인 ID
         */
        String loginId,

        /**
         * 이메일/앱으로 받은 OTP 코드
         */
        String otpCode
) {}
