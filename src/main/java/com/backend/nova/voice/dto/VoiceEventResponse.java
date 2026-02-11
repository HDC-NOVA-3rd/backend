package com.backend.nova.voice.dto;

public record VoiceEventResponse(
        String result,
        String receivedAt
) {
    public static VoiceEventResponse ok(String receivedAt) {
        return new VoiceEventResponse("SUCCESS", receivedAt);
    }
}
