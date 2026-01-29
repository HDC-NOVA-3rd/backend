package com.backend.nova.safety.dto;

import com.backend.nova.safety.enums.SafetyStatus;
import com.backend.nova.safety.enums.SensorType;

import java.time.LocalDateTime;

public record SafetyEventLogResponse(
        String dongNo,
        String facilityName,
        boolean manual,
        String requestFrom,
        String sensorName,
        SensorType sensorType,
        Double value,
        String unit,
        SafetyStatus statusTo,
        LocalDateTime eventAt
) {
}
