package com.backend.nova.voice.controller;

import com.backend.nova.voice.dto.VoiceAudioCommandRequest;
import com.backend.nova.voice.dto.VoiceAudioCommandResponse;
import com.backend.nova.voice.dto.VoiceEventRequest;
import com.backend.nova.voice.dto.VoiceEventResponse;
import com.backend.nova.voice.service.VoiceCommandService;
import com.backend.nova.voice.service.VoiceEventService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/voice")
@RequiredArgsConstructor
public class VoiceController {

    private final VoiceCommandService voiceCommandService;
    private final VoiceEventService voiceEventService;

    @Operation(summary = "음성 턴 처리", description = "분절된 오디오를 받아 STT/의도 처리 후 어시스턴트 응답을 반환합니다.")
    @PostMapping(value = "/turn", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public VoiceAudioCommandResponse turn(
            @RequestPart("audio") MultipartFile audio,
            @RequestParam String deviceId,
            @RequestParam Long memberId,
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) String requestId,
            @RequestParam(defaultValue = "ko-KR") String locale,
            @RequestParam(required = false) String timestamp,
            @RequestParam(required = false) String mockText
    ) {
        VoiceAudioCommandRequest request = new VoiceAudioCommandRequest(
                deviceId,
                memberId,
                sessionId,
                requestId,
                locale,
                timestamp,
                mockText
        );
        return voiceCommandService.handleAudioCommand(audio, request);
    }

    @Operation(summary = "음성 이벤트 수신", description = "엣지 디바이스의 재생/액션 상태 이벤트를 수신합니다.")
    @PostMapping("/events")
    public ResponseEntity<VoiceEventResponse> events(@RequestBody @Valid VoiceEventRequest request) {
        return ResponseEntity.ok(voiceEventService.acceptEvent(request));
    }
}
