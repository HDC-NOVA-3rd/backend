package com.backend.nova.mqtt;

import com.backend.nova.voice.dto.VoiceAudioCommandResponse;
import com.backend.nova.voice.dto.VoiceMqttRequest;
import com.backend.nova.voice.service.VoiceCommandService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class MqttVoiceInboundHandler {

    private static final String PREFIX = "hdc";

    private final ObjectMapper objectMapper;
    private final VoiceCommandService voiceCommandService;
    private final MessageChannel mqttOutboundChannel;

    public void handleMessage(Message<String> message) {
        String payload = message.getPayload();
        String topic = (String) message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC);

        log.info("MQTT voice inbound topic={}", topic);

        Long hoId = parseHoId(topic);
        if (hoId == null) {
            log.warn("MQTT voice inbound ignored: invalid topic={}", topic);
            return;
        }

        if (payload == null || payload.isBlank()) {
            log.warn("MQTT voice inbound ignored: empty payload topic={}", topic);
            publishResponse(hoId, errorResponse("음성 요청이 비어 있습니다.", "VOICE_BAD_REQUEST"));
            return;
        }

        VoiceMqttRequest req;
        try {
            req = objectMapper.readValue(payload, VoiceMqttRequest.class);
        } catch (Exception e) {
            log.error("MQTT voice payload parse failed. topic={}", topic, e);
            publishResponse(hoId, errorResponse("음성 요청 형식이 올바르지 않습니다.", "VOICE_BAD_REQUEST"));
            return;
        }

        if (req.audio() == null || req.audio().isBlank()) {
            log.warn("MQTT voice inbound ignored: audio field missing. topic={}", topic);
            publishResponse(hoId, errorResponse("오디오 데이터가 없습니다.", "VOICE_BAD_REQUEST"));
            return;
        }

        byte[] audioBytes;
        try {
            audioBytes = Base64.getDecoder().decode(req.audio());
        } catch (IllegalArgumentException e) {
            log.error("MQTT voice base64 decode failed. topic={}", topic, e);
            publishResponse(hoId, errorResponse("오디오 디코딩에 실패했습니다.", "VOICE_BAD_REQUEST"));
            return;
        }

        VoiceAudioCommandResponse response;
        try {
            response = voiceCommandService.handleAudioCommand(audioBytes, hoId, req.sessionId());
        } catch (Exception e) {
            log.error("MQTT voice command processing failed. hoId={}", hoId, e);
            publishResponse(hoId, errorResponse("음성 명령 처리 중 오류가 발생했습니다.", "VOICE_PROCESSING_ERROR"));
            return;
        }

        publishResponse(hoId, response);
    }

    private void publishResponse(Long hoId, VoiceAudioCommandResponse response) {
        String resTopic = PREFIX + "/" + hoId + "/assistant/voice/res";
        try {
            String resPayload = objectMapper.writeValueAsString(response);
            Message<String> outMsg = MessageBuilder.withPayload(resPayload)
                    .setHeader(MqttHeaders.TOPIC, resTopic)
                    .build();
            mqttOutboundChannel.send(outMsg);
            log.info("MQTT voice response published. topic={}, intent={}", resTopic, response.intent());
        } catch (Exception e) {
            log.error("MQTT voice response publish failed. topic={}", resTopic, e);
        }
    }

    private VoiceAudioCommandResponse errorResponse(String message, String intent) {
        return new VoiceAudioCommandResponse(
                "",
                UUID.randomUUID().toString(),
                "",
                message,
                message,
                intent,
                Map.of(),
                List.of(),
                false
        );
    }

    private Long parseHoId(String topic) {
        if (topic == null || topic.isBlank()) {
            return null;
        }

        String[] parts = topic.split("/");
        if (parts.length != 5 || !PREFIX.equalsIgnoreCase(parts[0])) {
            return null;
        }
        if (!"assistant".equalsIgnoreCase(parts[2])
                || !"voice".equalsIgnoreCase(parts[3])
                || !"req".equalsIgnoreCase(parts[4])) {
            return null;
        }

        try {
            return Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
