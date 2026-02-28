package com.backend.nova.member.dto;

public record RedisMember(
        Long memberId,
        String loginId,
        String name,
        Long apartmentId,
        Long hoId,
        String role
) {
}
