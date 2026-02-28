package com.backend.nova.voice.dto;

import java.util.Map;

public record VoiceActionResponse(
        String type,
        String target,
        String command,
        Map<String, Object> metadata
) {
}
