package com.backend.nova.voice.dto;

public record VoiceAudioCommandRequest(
        String deviceId,
        Long memberId,
        String sessionId,
        String requestId,
        String locale,
        String timestamp,
        String mockText
) {
}
