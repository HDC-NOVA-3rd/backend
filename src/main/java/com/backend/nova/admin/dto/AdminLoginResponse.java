package com.backend.nova.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminLoginResponse {

    private boolean otpRequired;
    private String accessToken;
    private String challengeToken;

    public static AdminLoginResponse success(String accessToken) {
        return new AdminLoginResponse(false, accessToken, null);
    }

    public static AdminLoginResponse otpRequired(String challengeToken) {
        return new AdminLoginResponse(true, null, challengeToken);
    }
}

