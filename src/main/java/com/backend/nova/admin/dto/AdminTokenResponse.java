package com.backend.nova.admin.dto;

import com.backend.nova.auth.jwt.JwtToken;
import lombok.Builder;

@Builder
public record AdminTokenResponse(
        String accessToken,
        String refreshToken,
        Long adminId,
        String name,
        String role
) {
}
