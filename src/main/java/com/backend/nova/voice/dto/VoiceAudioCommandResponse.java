package com.backend.nova.voice.dto;

import java.util.List;

public record VoiceAudioCommandResponse(
        String sessionId,
        String requestId,
        String recognizedText,
        String answer,
        String ttsText,
        String intent,
        Object data,
        List<VoiceActionResponse> actions,
        boolean endSession
) {
}
