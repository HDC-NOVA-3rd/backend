package com.backend.nova.safety.dto;

import com.backend.nova.safety.enums.SafetyReason;
import com.backend.nova.safety.enums.SafetyStatus;

public record SafetyLockResponse(
        Long facilityId,
        Boolean reservationAvailable,
        SafetyStatus status,
        SafetyReason reason
) {
}
