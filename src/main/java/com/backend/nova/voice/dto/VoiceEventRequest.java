package com.backend.nova.voice.dto;

import jakarta.validation.constraints.NotBlank;

public record VoiceEventRequest(
        @NotBlank String deviceId,
        String sessionId,
        String requestId,
        @NotBlank String eventType,
        @NotBlank String status,
        String detail,
        String timestamp
) {
}
