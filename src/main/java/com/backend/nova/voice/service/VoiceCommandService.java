package com.backend.nova.voice.service;

import com.backend.nova.chat.dto.ChatRequest;
import com.backend.nova.chat.dto.ChatResponse;
import com.backend.nova.chat.service.ChatService;
import com.backend.nova.global.exception.BusinessException;
import com.backend.nova.global.exception.ErrorCode;
import com.backend.nova.voice.dto.VoiceActionResponse;
import com.backend.nova.voice.dto.VoiceAudioCommandResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VoiceCommandService {

    private static final Logger log = LoggerFactory.getLogger(VoiceCommandService.class);

    private final HuggingFaceSpeechToTextService speechToTextService;
    private final VoiceDeviceMemberResolver voiceDeviceMemberResolver;
    private final ChatService chatService;

    public VoiceAudioCommandResponse handleAudioCommand(byte[] audioBytes, Long hoId, String sessionId) {
        String traceId = UUID.randomUUID().toString();

        String recognizedText = speechToTextService.transcribe(audioBytes);
        if (recognizedText == null || recognizedText.isBlank()) {
            String fallback = "I could not recognize speech.";
            return new VoiceAudioCommandResponse(
                    sessionId,
                    traceId,
                    "",
                    fallback,
                    fallback,
                    "STT_EMPTY",
                    Map.of("hoId", hoId),
                    List.of(),
                    false
            );
        }

        Long memberId = voiceDeviceMemberResolver.resolveMemberId(hoId);

        ChatResponse chatResponse;
        try {
            chatResponse = chatService.chat(
                    new ChatRequest(recognizedText, sessionId, memberId)
            );
        } catch (IllegalArgumentException e) {
            log.warn("Voice request validation failed: {}", e.getMessage());
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        String responseTraceId = extractTraceId(chatResponse.data());
        if (responseTraceId != null && !responseTraceId.isBlank()) {
            traceId = responseTraceId;
        }

        return new VoiceAudioCommandResponse(
                chatResponse.sessionId(),
                traceId,
                recognizedText,
                chatResponse.answer(),
                chatResponse.answer(),
                chatResponse.intent(),
                chatResponse.data(),
                buildActions(chatResponse),
                false
        );
    }

    private String extractTraceId(Object data) {
        if (!(data instanceof Map<?, ?> map)) {
            return null;
        }
        Object traceId = map.get("traceId");
        if (traceId == null) {
            return null;
        }
        return String.valueOf(traceId);
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
}
