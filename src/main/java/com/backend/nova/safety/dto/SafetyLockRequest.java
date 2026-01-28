package com.backend.nova.safety.dto;

import com.backend.nova.safety.enums.SafetyLockCommand;

public record SafetyLockRequest(
        Long facilityId,
        SafetyLockCommand command
) {
}
