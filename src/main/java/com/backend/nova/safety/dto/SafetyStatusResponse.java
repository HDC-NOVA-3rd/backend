package com.backend.nova.safety.dto;

import com.backend.nova.safety.enums.SafetyReason;
import com.backend.nova.safety.enums.SafetyStatus;

import java.time.LocalDateTime;

public record SafetyStatusResponse(
        String dongNo,
        String facilityName,
        SafetyStatus status,
        SafetyReason reason,
        LocalDateTime updatedAt
) {
}
