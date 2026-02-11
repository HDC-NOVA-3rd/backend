package com.backend.nova.voice.service;

import com.backend.nova.voice.dto.VoiceAudioCommandRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@ConditionalOnProperty(prefix = "voice.stt", name = "provider", havingValue = "fallback", matchIfMissing = true)
public class FallbackSpeechToTextService implements SpeechToTextService {

    private static final Logger log = LoggerFactory.getLogger(FallbackSpeechToTextService.class);

    @Override
    public String transcribe(MultipartFile audioFile, VoiceAudioCommandRequest request) {
        if (request.mockText() != null && !request.mockText().isBlank()) {
            return request.mockText().trim();
        }

        log.warn(
                "STT provider is not configured. Returning empty transcript. deviceId={}, file={}",
                request.deviceId(),
                audioFile.getOriginalFilename()
        );
        return "";
    }
}
