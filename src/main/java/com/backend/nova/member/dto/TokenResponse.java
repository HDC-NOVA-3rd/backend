package com.backend.nova.member.dto;

import lombok.Builder;

@Builder
public record TokenResponse(
        String accessToken,
        String refreshToken,
        Long id,        // Member.id 또는 Admin.id
        String loginId,
        String name,
        String role     // MEMBER / ADMIN / SUPER_ADMIN
) {}
