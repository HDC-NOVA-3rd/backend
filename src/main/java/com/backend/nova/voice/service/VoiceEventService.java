package com.backend.nova.voice.service;

import com.backend.nova.voice.dto.VoiceEventRequest;
import com.backend.nova.voice.dto.VoiceEventResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class VoiceEventService {

    private static final Logger log = LoggerFactory.getLogger(VoiceEventService.class);

    public VoiceEventResponse acceptEvent(VoiceEventRequest request) {
        log.info(
                "Voice event received deviceId={}, sessionId={}, requestId={}, eventType={}, status={}, detail={}",
                request.deviceId(),
                request.sessionId(),
                request.requestId(),
                request.eventType(),
                request.status(),
                request.detail()
        );

        return VoiceEventResponse.ok(Instant.now().toString());
    }
}
