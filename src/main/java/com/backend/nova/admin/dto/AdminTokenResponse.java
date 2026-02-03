package com.backend.nova.admin.dto;

import lombok.Builder;

@Builder
public record AdminTokenResponse(
        String accessToken,
        String refreshToken,
        Long adminId,
        String name
) {
}

