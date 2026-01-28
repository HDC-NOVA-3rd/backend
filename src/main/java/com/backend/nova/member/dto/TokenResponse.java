package com.backend.nova.member.dto;

import lombok.Builder;

@Builder
public record TokenResponse(
        String grantType,
        String accessToken,
        String refreshToken
) {
}
