package com.backend.nova.safety.dto;

public record SafetySensorInboundPayload(
        String sensorType,
        Double value,
        String unit,
        String ts
) {
    public boolean isValid() {
        return sensorType != null && !sensorType.isBlank()
                && value != null
                && unit != null && !unit.isBlank()
                && ts != null && !ts.isBlank();
    }
}
