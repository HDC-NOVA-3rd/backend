package com.backend.nova.member.dto;

public record ResetPWRequest(
        String loginId,
        String name,
        String phoneNumber
) {
}
