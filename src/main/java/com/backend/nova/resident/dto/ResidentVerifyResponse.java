package com.backend.nova.resident.dto;

import com.backend.nova.member.entity.LoginType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public record ResidentVerifyResponse(
        boolean isVerified,
        Long residentId,
        String name,
        SignupStatus status,
        LoginType loginType
) {
}