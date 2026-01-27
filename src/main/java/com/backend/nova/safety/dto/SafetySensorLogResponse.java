package com.backend.nova.safety.dto;

import com.backend.nova.safety.enums.SensorType;

public record SafetySensorLogResponse(
        Long id,
        Long sensorId,
        SensorType sensorType,
        Double value
) {
}
