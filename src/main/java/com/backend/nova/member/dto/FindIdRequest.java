package com.backend.nova.member.dto;

public record FindIdRequest(
        String name,
        String phoneNumber
) {
}
