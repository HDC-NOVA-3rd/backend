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
                && unit != null && !unit.isBlank();
    }

    // ts를 서버에서 채우는
    public SafetySensorInboundPayload setTS() {
        return new SafetySensorInboundPayload(sensorType, value, unit, java.time.Instant.now().toString());
    }
}
