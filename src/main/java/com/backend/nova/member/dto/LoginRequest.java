package com.backend.nova.member.dto;

public record LoginRequest(
        String loginId,
        String password
) {
}
