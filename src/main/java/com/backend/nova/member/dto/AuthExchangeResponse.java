package com.backend.nova.member.dto;

import lombok.Builder;

@Builder
public record AuthExchangeResponse(
        String type, // "LOGIN" 또는 "REGISTER"
        TokenResponse tokenResponse, // 로그인 성공 시 사용
        String registerToken // 회원가입 필요 시 사용
) {
    // 로그인 성공 시 응답 생성 메서드
    public static AuthExchangeResponse login(TokenResponse tokenResponse) {
        return AuthExchangeResponse.builder()
                .type("LOGIN")
                .tokenResponse(tokenResponse)
                .build();
    }

    // 회원가입 필요 시 응답 생성 메서드
    public static AuthExchangeResponse register(String registerToken) {
        return AuthExchangeResponse.builder()
                .type("REGISTER")
                .registerToken(registerToken)
                .build();
    }
}
