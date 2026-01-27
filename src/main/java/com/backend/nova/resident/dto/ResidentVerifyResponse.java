package com.backend.nova.resident.dto;

public record ResidentVerifyResponseDto(
        boolean isVerified,
        Long residentId,
        String message
) {
}