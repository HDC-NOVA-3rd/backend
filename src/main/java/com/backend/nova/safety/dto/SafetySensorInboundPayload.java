package com.backend.nova.safety.dto;

public record SafetySensorInboundPayload(
        String deviceId,
        String sensorType,
        Double value,
        String unit,
        String ts
) {
    public boolean isValid() {
        return deviceId != null && !deviceId.isBlank()
                && sensorType != null && !sensorType.isBlank()
                && value != null
                && unit != null && !unit.isBlank()
                && ts != null && !ts.isBlank();
    }
}
