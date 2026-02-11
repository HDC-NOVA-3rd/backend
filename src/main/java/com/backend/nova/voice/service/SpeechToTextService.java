package com.backend.nova.voice.service;

import com.backend.nova.voice.dto.VoiceAudioCommandRequest;
import org.springframework.web.multipart.MultipartFile;

public interface SpeechToTextService {
    String transcribe(MultipartFile audioFile, VoiceAudioCommandRequest request);
}
