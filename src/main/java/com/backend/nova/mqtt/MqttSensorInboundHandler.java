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
public class MqttSensorInboundHandler {

    private static final String PREFIX = "hdc";
    private static final String DEVICE_SEGMENT = "device";
    private static final String SAFETY_SEGMENT = "safety";
    private static final String ENV_SEGMENT = "env";
    private static final String DATA_SEGMENT = "data";

    private final ObjectMapper objectMapper;
    private final SafetyService safetyService;

    @ServiceActivator(inputChannel = "mqttSensorInputChannel")
    public void handleMessage(Message<String> message) {
        String payload = message.getPayload();
        String topic = (String) message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC);

        log.info("MQTT sensor inbound topic={}, payload={}", topic, payload);

        SensorTopicContext context = parseSensorTopic(topic);
        if (context == null) {
            log.warn("MQTT sensor inbound ignored: invalid topic={}", topic);
            return;
        }

        if (SAFETY_SEGMENT.equalsIgnoreCase(context.domain())) {
            SafetySensorInboundPayload inboundPayload = parseSafetyPayload(payload);
            if (inboundPayload == null || !inboundPayload.isValid()) {
                log.warn("MQTT safety ignored: invalid payload topic={}, payload={}", topic, payload);
                return;
            }
            safetyService.handleSafetySensor(context.deviceId(), inboundPayload);
            return;
        }

        if (ENV_SEGMENT.equalsIgnoreCase(context.domain())) {
            if (payload == null || payload.isBlank()) {
                log.warn("MQTT env ignored: empty payload topic={}", topic);
                return;
            }
            log.info("MQTT env received deviceId={} topic={}", context.deviceId(), topic);
            return;
        }

        log.warn("MQTT sensor inbound ignored: unsupported domain={} topic={}", context.domain(), topic);
    }

    private SensorTopicContext parseSensorTopic(String topic) {
        if (topic == null || topic.isBlank()) {
            return null;
        }
        String[] parts = topic.split("/");
        if (parts.length != 5 || !PREFIX.equalsIgnoreCase(parts[0])) {
            return null;
        }
        if (!DEVICE_SEGMENT.equalsIgnoreCase(parts[1]) || !DATA_SEGMENT.equalsIgnoreCase(parts[4])) {
            return null;
        }
        String deviceId = parts[2];
        String domain = parts[3];
        if (deviceId == null || deviceId.isBlank() || domain == null || domain.isBlank()) {
            return null;
        }
        return new SensorTopicContext(deviceId, domain);
    }

    private SafetySensorInboundPayload parseSafetyPayload(String payload) {
        try {
            return objectMapper.readValue(payload, SafetySensorInboundPayload.class);
        } catch (JsonProcessingException e) {
            log.warn("MQTT safety payload parse failed: {}", e.getMessage());
            return null;
        }
    }

    private record SensorTopicContext(String deviceId, String domain) {
    }
}
