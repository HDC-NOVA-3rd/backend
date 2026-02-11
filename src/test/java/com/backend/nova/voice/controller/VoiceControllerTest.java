package com.backend.nova.voice.controller;

import com.backend.nova.ControllerTestSupport;
import com.backend.nova.voice.dto.VoiceActionResponse;
import com.backend.nova.voice.dto.VoiceAudioCommandResponse;
import com.backend.nova.voice.dto.VoiceEventRequest;
import com.backend.nova.voice.dto.VoiceEventResponse;
import com.backend.nova.voice.service.VoiceCommandService;
import com.backend.nova.voice.service.VoiceEventService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VoiceController.class)
class VoiceControllerTest extends ControllerTestSupport {

    @MockitoBean
    private VoiceCommandService voiceCommandService;

    @MockitoBean
    private VoiceEventService voiceEventService;

    @Test
    @DisplayName("audio-commands multipart request returns voice response")
    void audioCommands_Success() throws Exception {
        VoiceAudioCommandResponse response = new VoiceAudioCommandResponse(
                "session-1",
                "request-1",
                "turn on the light",
                "Turning on the light.",
                "Turning on the light.",
                "DEVICE_CONTROL",
                Map.of("traceId", "trace-1"),
                List.of(new VoiceActionResponse("MQTT", "assistant", "EXECUTE", Map.of("traceId", "trace-1"))),
                false
        );
        given(voiceCommandService.handleAudioCommand(any(), any())).willReturn(response);

        MockMultipartFile audio = new MockMultipartFile(
                "audio",
                "sample.wav",
                "audio/wav",
                "wav-bytes".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(
                        multipart("/api/voice/turn")
                                .file(audio)
                                .param("deviceId", "pi-01")
                                .param("memberId", "1")
                                .param("sessionId", "session-1")
                                .param("requestId", "request-1")
                                .param("locale", "ko-KR")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recognizedText").value("turn on the light"))
                .andExpect(jsonPath("$.answer").value("Turning on the light."))
                .andExpect(jsonPath("$.actions[0].type").value("MQTT"));
    }

    @Test
    @DisplayName("events endpoint accepts payload and returns ack")
    void events_Accepted() throws Exception {
        VoiceEventRequest request = new VoiceEventRequest(
                "pi-01",
                "session-1",
                "request-1",
                "ACTION_RESULT",
                "SUCCESS",
                "light=ON",
                "2026-02-11T10:00:00+09:00"
        );

        given(voiceEventService.acceptEvent(any()))
                .willReturn(new VoiceEventResponse("SUCCESS", "2026-02-11T01:00:00Z"));

        mockMvc.perform(
                        post("/api/voice/events")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"));
    }

    @Test
    @DisplayName("events endpoint returns 400 when required fields are missing")
    void events_InvalidRequest() throws Exception {
        mockMvc.perform(
                        post("/api/voice/events")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andExpect(status().isBadRequest());
    }
}
