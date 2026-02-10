package com.backend.nova.safety.dto;

import com.backend.nova.safety.enums.SafetyReason;
import com.backend.nova.safety.enums.SafetyStatus;
import com.backend.nova.safety.enums.SensorType;

import java.time.LocalDateTime;

public record SafetyMqttUpdatePayload(
        String dongNo,
        String facilityName,
        SafetyStatus status,
        SafetyReason reason,
        LocalDateTime updatedAt,
        Long sensorId,
        String hoNo,
        String spaceName,
        String sensorName,
        SensorType sensorType,
        Double value,
        String unit,
        LocalDateTime recordedAt
) {
}
