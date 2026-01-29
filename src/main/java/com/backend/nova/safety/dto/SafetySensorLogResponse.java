package com.backend.nova.safety.dto;

import com.backend.nova.safety.enums.SensorType;

import java.time.LocalDateTime;

public record SafetySensorLogResponse(
        String sensorName,
        String dongNo,
        String facilityName,
        SensorType sensorType,
        Double value,
        String unit,
        LocalDateTime eventedAt
) {
}
