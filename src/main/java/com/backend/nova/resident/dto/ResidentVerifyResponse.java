package com.backend.nova.resident.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResidentVerifyResponse(
        boolean isVerified,
        Long residentId,
        String message
) {
}