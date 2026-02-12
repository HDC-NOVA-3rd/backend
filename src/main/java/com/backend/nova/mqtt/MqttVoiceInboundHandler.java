package com.backend.nova.mqtt;

import com.backend.nova.voice.dto.VoiceAudioCommandResponse;
import com.backend.nova.voice.service.VoiceCommandService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Service;

import java.util.Base64;

/**
 * MQTT 음성 턴 처리 핸들러.
 *
 * <p>구독: {@code hdc/{hoId}/assistant/voice/req}
 * <pre>
 * payload(JSON): { "audio": "<base64 WAV>", "sessionId": "..." }
 * </pre>
 *
 * <p>응답 발행: {@code hdc/{hoId}/assistant/voice/res}
 * <pre>
 * payload(JSON): VoiceAudioCommandResponse
 * </pre>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MqttVoiceInboundHandler {

    private static final String PREFIX = "hdc";

    private final ObjectMapper objectMapper;
    private final VoiceCommandService voiceCommandService;
    private final MessageChannel mqttAssistantOutboundChannel;

    @ServiceActivator(inputChannel = "mqttVoiceInputChannel")
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
            return;
        }

        VoiceRequest req;
        try {
            req = objectMapper.readValue(payload, VoiceRequest.class);
        } catch (Exception e) {
            log.error("MQTT voice payload parse failed. topic={}", topic, e);
            return;
        }

        if (req.audio == null || req.audio.isBlank()) {
            log.warn("MQTT voice inbound ignored: audio field missing. topic={}", topic);
            return;
        }

        byte[] audioBytes;
        try {
            audioBytes = Base64.getDecoder().decode(req.audio);
        } catch (IllegalArgumentException e) {
            log.error("MQTT voice base64 decode failed. topic={}", topic, e);
            return;
        }

        VoiceAudioCommandResponse response;
        try {
            response = voiceCommandService.handleAudioCommand(audioBytes, hoId, req.sessionId);
        } catch (Exception e) {
            log.error("MQTT voice command processing failed. hoId={}", hoId, e);
            return;
        }

        // 응답을 MQTT로 발행
        String resTopic = PREFIX + "/" + hoId + "/assistant/voice/res";
        try {
            String resPayload = objectMapper.writeValueAsString(response);
            Message<String> outMsg = MessageBuilder.withPayload(resPayload)
                    .setHeader(MqttHeaders.TOPIC, resTopic)
                    .build();
            mqttAssistantOutboundChannel.send(outMsg);
            log.info("MQTT voice response published. topic={}, intent={}", resTopic, response.intent());
        } catch (Exception e) {
            log.error("MQTT voice response publish failed. topic={}", resTopic, e);
        }
    }

    private Long parseHoId(String topic) {
        if (topic == null || topic.isBlank()) return null;

        // hdc/{hoId}/assistant/voice/req
        String[] parts = topic.split("/");
        if (parts.length != 5 || !PREFIX.equalsIgnoreCase(parts[0])) return null;
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

    @Data
    private static class VoiceRequest {
        private String audio;      // base64 encoded WAV
        private String sessionId;
    }
}
