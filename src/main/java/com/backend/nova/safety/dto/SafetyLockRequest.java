package com.backend.nova.safety.dto;

public record SafetyLockRequest(
        Long facilityId,
        Boolean reservationAvailable
) {
}
