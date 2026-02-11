package com.backend.nova.voice.service;

import com.backend.nova.chat.dto.ChatRequest;
import com.backend.nova.chat.dto.ChatResponse;
import com.backend.nova.chat.service.ChatService;
import com.backend.nova.global.exception.BusinessException;
import com.backend.nova.global.exception.ErrorCode;
import com.backend.nova.voice.dto.VoiceActionResponse;
import com.backend.nova.voice.dto.VoiceAudioCommandRequest;
import com.backend.nova.voice.dto.VoiceAudioCommandResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VoiceCommandService {

    private static final Logger log = LoggerFactory.getLogger(VoiceCommandService.class);
    private static final Set<String> SUPPORTED_AUDIO_EXTENSIONS = Set.of(
            "wav", "mp3", "m4a", "ogg", "webm", "aac"
    );

    private final SpeechToTextService speechToTextService;
    private final ChatService chatService;

    public VoiceAudioCommandResponse handleAudioCommand(MultipartFile audioFile, VoiceAudioCommandRequest request) {
        validateAudioFile(audioFile);
        String requestId = normalizeRequestId(request.requestId());

        String recognizedText = speechToTextService.transcribe(audioFile, request);
        if (recognizedText == null || recognizedText.isBlank()) {
            String fallback = "I could not recognize speech.";
            return new VoiceAudioCommandResponse(
                    normalizeSessionId(request.sessionId()),
                    requestId,
                    "",
                    fallback,
                    fallback,
                    "STT_EMPTY",
                    Map.of("deviceId", request.deviceId()),
                    List.of(),
                    false
            );
        }

        ChatResponse chatResponse;
        try {
            chatResponse = chatService.chat(
                    new ChatRequest(recognizedText, request.sessionId(), request.memberId())
            );
        } catch (IllegalArgumentException e) {
            log.warn("Voice request validation failed: {}", e.getMessage());
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        return new VoiceAudioCommandResponse(
                chatResponse.sessionId(),
                requestId,
                recognizedText,
                chatResponse.answer(),
                chatResponse.answer(),
                chatResponse.intent(),
                chatResponse.data(),
                buildActions(chatResponse),
                false
        );
    }

    private List<VoiceActionResponse> buildActions(ChatResponse chatResponse) {
        if (!"DEVICE_CONTROL".equalsIgnoreCase(chatResponse.intent())) {
            return List.of();
        }
        if (!(chatResponse.data() instanceof Map<?, ?> data)) {
            return List.of();
        }

        Object traceId = data.get("traceId");
        Map<String, Object> metadata = traceId == null
                ? Map.of("intent", chatResponse.intent())
                : Map.of("intent", chatResponse.intent(), "traceId", traceId);

        return List.of(
                new VoiceActionResponse(
                        "MQTT",
                        "assistant",
                        "EXECUTE",
                        metadata
                )
        );
    }

    private void validateAudioFile(MultipartFile audioFile) {
        if (audioFile == null || audioFile.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        String filename = audioFile.getOriginalFilename();
        String contentType = audioFile.getContentType();
        boolean contentTypeAudio = contentType != null && contentType.startsWith("audio/");

        if (filename == null || filename.isBlank() || !filename.contains(".")) {
            if (!contentTypeAudio) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST);
            }
            return;
        }

        String extension = filename.substring(filename.lastIndexOf('.') + 1)
                .toLowerCase(Locale.ROOT);
        if (!SUPPORTED_AUDIO_EXTENSIONS.contains(extension) && !contentTypeAudio) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    private String normalizeRequestId(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return requestId;
    }

    private String normalizeSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return "";
        }
        return sessionId;
    }
}
