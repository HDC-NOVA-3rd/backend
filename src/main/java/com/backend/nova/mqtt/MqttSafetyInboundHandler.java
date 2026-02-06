package com.backend.nova.mqtt;

import com.backend.nova.safety.dto.SafetySensorInboundPayload;
import com.backend.nova.safety.service.SafetyService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class MqttSafetyInboundHandler {

    private final ObjectMapper objectMapper;
    private final SafetyService safetyService;

    @ServiceActivator(inputChannel = "mqttSafetyInputChannel")
    public void handleSafetyMessage(Message<String> message) {
        String payload = message.getPayload();
        String topic = (String) message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC);

        log.info("MQTT safety inbound topic={}, payload={}", topic, payload);

        if (payload == null || payload.isBlank()) {
            log.warn("MQTT safety ignored: empty payload topic={}", topic);
            return;
        }

        String deviceId = extractDeviceId(topic);
        if (deviceId == null) {
            log.warn("MQTT safety inbound ignored: invalid topic={}", topic);
            return;
        }

        try {
            SafetySensorInboundPayload inboundPayload =
                    objectMapper.readValue(payload, SafetySensorInboundPayload.class);
            if (!inboundPayload.isValid()) {
                log.warn("MQTT safety ignored: invalid payload topic={}, payload={}", topic, payload);
                return;
            }
            safetyService.handleSafetySensor(deviceId, inboundPayload);
        } catch (JsonProcessingException e) {
            log.warn("MQTT safety payload parse failed: {}", e.getMessage());
        }
    }

    private String extractDeviceId(String topic) {
        String[] parts = topic.split("/");
        String deviceId = parts[2];
        return deviceId == null || deviceId.isBlank() ? null : deviceId;
    }
}
