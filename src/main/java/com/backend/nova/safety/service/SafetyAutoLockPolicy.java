package com.backend.nova.safety.service;

import com.backend.nova.safety.enums.SensorType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SafetyAutoLockPolicy {

    private final Double smokeThreshold;
    private final Double heatThreshold;

    public SafetyAutoLockPolicy(
            @Value("${safety.auto-lock.threshold.smoke:#{null}}") Double smokeThreshold,
            @Value("${safety.auto-lock.threshold.heat:#{null}}") Double heatThreshold
    ) {
        this.smokeThreshold = smokeThreshold;
        this.heatThreshold = heatThreshold;
    }

    public boolean isDangerous(SensorType sensorType, Double value) {
        if (sensorType == null || value == null) {
            return false;
        }
        Double threshold = thresholdFor(sensorType);
        if (threshold == null) {
            return false;
        }
        return value >= threshold;
    }

    public String unitFor(SensorType sensorType) {
        if (sensorType == null) {
            return null;
        }
        return switch (sensorType) {
            case SMOKE -> "ppm";
            case HEAT -> "°C";
        };
    }

    private Double thresholdFor(SensorType sensorType) {
        return switch (sensorType) {
            case SMOKE -> smokeThreshold;
            case HEAT -> heatThreshold;
        };
    }
}

