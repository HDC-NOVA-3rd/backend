package com.backend.nova.voice.dto;

public record VoiceMqttRequest(
        String audio,
        String sessionId
) {
}
