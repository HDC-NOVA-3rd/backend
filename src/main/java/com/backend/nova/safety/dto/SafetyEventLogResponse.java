package com.backend.nova.safety.dto;

import com.backend.nova.safety.enums.SafetyStatus;
import com.backend.nova.safety.enums.SensorType;

import java.time.LocalDateTime;

public record SafetyEventLogResponse(
        Long id,
        String dongNo,
        String facilityName,
        boolean manual,
        String requestFrom,
        SensorType sensorType,
        Double value,
        String unit,
        SafetyStatus statusTo,
        LocalDateTime eventAt
) {
}
