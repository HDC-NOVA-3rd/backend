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
            String fallback = "음성인식 실패했습니다.";
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
            if (sessionId != null && !sessionId.isBlank()) {
                log.warn("Voice session invalid. retrying with new session. sessionId={}, reason={}", sessionId, e.getMessage());
                try {
                    chatResponse = chatService.chat(
                            new ChatRequest(recognizedText, null, memberId)
                    );
                } catch (IllegalArgumentException retryException) {
                    log.warn("Voice request validation failed after retry: {}", retryException.getMessage());
                    throw new BusinessException(ErrorCode.INVALID_REQUEST);
                }
            } else {
                log.warn("Voice request validation failed: {}", e.getMessage());
                throw new BusinessException(ErrorCode.INVALID_REQUEST);
            }
        }

        String responseTraceId = extractTraceId(chatResponse.data());
        if (responseTraceId != null && !responseTraceId.isBlank()) {
            traceId = responseTraceId;
        }

        String replyText = normalizeReplyText(chatResponse.answer());

        return new VoiceAudioCommandResponse(
                chatResponse.sessionId(),
                traceId,
                recognizedText,
                replyText,
                replyText,
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

    private String normalizeReplyText(String answer) {
        if (answer != null && !answer.isBlank()) {
            return answer;
        }
        return "답변을 준비하지 못했어요. 다시 말씀해 주세요.";
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
