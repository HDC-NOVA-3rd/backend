package com.backend.nova.safety.dto;

import com.backend.nova.safety.enums.SafetyReason;
import com.backend.nova.safety.enums.SafetyStatus;

public record SafetyLockResponse(
        String facilityName,
        Boolean reservationAvailable,
        SafetyStatus status,
        SafetyReason reason
) {
}
