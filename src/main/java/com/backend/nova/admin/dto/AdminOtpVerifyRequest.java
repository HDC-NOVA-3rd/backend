package com.backend.nova.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminOtpVerifyRequest {

    /**
     * 관리자 로그인 ID
     */
    private String loginId;

    /**
     * 이메일/앱으로 받은 OTP 코드
     */
    private String otpCode;
}
