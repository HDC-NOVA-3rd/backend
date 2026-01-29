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
public class MqttInboundHandler {

    private static final String PREFIX = "hdc";
    private static final String DEVICE_SEGMENT = "device";
    private static final String SAFETY_SEGMENT = "safety";
    private static final String DATA_SEGMENT = "data";

    private final ObjectMapper objectMapper;
    private final SafetyService safetyService;

    @ServiceActivator(inputChannel = "mqttInputChannel")
    public void handleMessage(Message<String> message) {
        String payload = message.getPayload();
        String topic = (String) message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC);

        log.info("MQTT inbound topic={}, payload={}", topic, payload);

        TopicDevice topicDevice = parseTopic(topic);
        if (topicDevice == null) {
            log.warn("MQTT inbound ignored: invalid topic={}", topic);
            return;
        }

        SafetySensorInboundPayload inboundPayload = parsePayload(payload);
        if (inboundPayload == null || !inboundPayload.isValid()) {
            log.warn("MQTT inbound ignored: invalid payload topic={}, payload={}", topic, payload);
            return;
        }

        if (!topicDevice.deviceId().equals(inboundPayload.deviceId())) {
            log.warn("MQTT inbound ignored: topic deviceId mismatch topic={}, payloadDeviceId={}",
                    topic, inboundPayload.deviceId());
            return;
        }

        safetyService.handleSafetySensor(topicDevice.deviceId(), inboundPayload);
    }

    private TopicDevice parseTopic(String topic) {
        if (topic == null || topic.isBlank()) {
            return null;
        }
        String[] parts = topic.split("/");
        if (parts.length != 5) {
            return null;
        }
        if (!PREFIX.equalsIgnoreCase(parts[0])) {
            return null;
        }
        if (!DEVICE_SEGMENT.equalsIgnoreCase(parts[1])) {
            return null;
        }
        if (!SAFETY_SEGMENT.equalsIgnoreCase(parts[3])) {
            return null;
        }
        if (!DATA_SEGMENT.equalsIgnoreCase(parts[4])) {
            return null;
        }
        String deviceId = parts[2];
        if (deviceId == null || deviceId.isBlank()) {
            return null;
        }
        return new TopicDevice(deviceId);
    }

    private SafetySensorInboundPayload parsePayload(String payload) {
        try {
            return objectMapper.readValue(payload, SafetySensorInboundPayload.class);
        } catch (JsonProcessingException e) {
            log.warn("MQTT inbound payload parse failed: {}", e.getMessage());
            return null;
        }
    }

    private record TopicDevice(String deviceId) {
    }
}
