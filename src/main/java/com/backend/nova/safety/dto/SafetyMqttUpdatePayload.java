package com.backend.nova.safety.dto;

import com.backend.nova.safety.enums.SafetyReason;
import com.backend.nova.safety.enums.SafetyStatus;
import com.backend.nova.safety.enums.SensorType;

import java.time.LocalDateTime;

public record SafetyMqttUpdatePayload(
        Long dongId,
        String dongNo,
        Long facilityId,
        String facilityName,
        SafetyStatus statusTo,          // 프론트엔드 호환성을 위해 이름 변경 status -> statusTo
        SafetyReason reason,
        LocalDateTime eventAt,         // 프론트엔드 호환성을 위해 이름 변경 updatedAt -> eventAt
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
