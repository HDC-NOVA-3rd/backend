package com.backend.nova.voice.service;

import com.backend.nova.chat.service.ChatService;
import com.backend.nova.voice.dto.VoiceAudioCommandResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class VoiceCommandServiceTest {

    @Mock
    private HuggingFaceSpeechToTextService speechToTextService;

    @Mock
    private VoiceDeviceMemberResolver voiceDeviceMemberResolver;

    @Mock
    private ChatService chatService;

    @InjectMocks
    private VoiceCommandService voiceCommandService;

    @Test
    @DisplayName("processes wav audio bytes")
    void handleAudioCommand_acceptsWav() {
        given(speechToTextService.transcribe(any(byte[].class))).willReturn("");

        byte[] audioBytes = "wav-bytes".getBytes(StandardCharsets.UTF_8);

        VoiceAudioCommandResponse response = voiceCommandService.handleAudioCommand(audioBytes, 1L, "session-1");

        assertThat(response.intent()).isEqualTo("STT_EMPTY");
        verify(speechToTextService).transcribe(any(byte[].class));
        verifyNoInteractions(voiceDeviceMemberResolver);
        verifyNoInteractions(chatService);
    }

    @Test
    @DisplayName("calls STT with raw bytes")
    void handleAudioCommand_rawBytes() {
        given(speechToTextService.transcribe(any(byte[].class))).willReturn("");

        byte[] audioBytes = "mp3-bytes".getBytes(StandardCharsets.UTF_8);

        VoiceAudioCommandResponse response = voiceCommandService.handleAudioCommand(audioBytes, 1L, "session-1");

        assertThat(response.intent()).isEqualTo("STT_EMPTY");
        verify(speechToTextService).transcribe(any(byte[].class));
        verifyNoInteractions(voiceDeviceMemberResolver);
        verifyNoInteractions(chatService);
    }
}
