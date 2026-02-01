package com.backend.nova.auth.jwt;
import lombok.Builder;

@Builder
public record JwtToken(
        String grantType,
        String accessToken,
        String refreshToken
) {
}
