package com.backend.nova.member.dto;

import com.backend.nova.auth.jwt.JwtToken;
import lombok.Builder;

@Builder
public record TokenResponse(
        String accessToken,
        String refreshToken,
        Long memberId,
        String name
) {
}
