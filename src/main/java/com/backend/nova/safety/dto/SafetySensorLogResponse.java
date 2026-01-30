package com.backend.nova.safety.dto;

import com.backend.nova.safety.enums.SensorType;

import java.time.LocalDateTime;

public record SafetySensorLogResponse(
        Long id,
        Long sensorId,
        SensorType sensorType,
        Double value,
        LocalDateTime createdAt
) {
}
