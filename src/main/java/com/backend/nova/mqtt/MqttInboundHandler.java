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
    private static final String ENV_SEGMENT = "env";
    private static final String DATA_SEGMENT = "data";
    private static final String ASSISTANT_SEGMENT = "assistant";
    private static final String EXECUTE_SEGMENT = "execute";
    private static final String RES_SEGMENT = "res";

    private final ObjectMapper objectMapper;
    private final SafetyService safetyService;

    @ServiceActivator(inputChannel = "mqttInputChannel")
    public void handleMessage(Message<String> message) {
        String payload = message.getPayload();
        String topic = (String) message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC);

        log.info("MQTT inbound topic={}, payload={}", topic, payload);

        TopicContext context = parseTopic(topic);
        if (context == null) {
            log.warn("MQTT inbound ignored: invalid topic={}", topic);
            return;
        }

        if (context.isSensorData()) {
            handleSensorData(context, payload, topic);
            return;
        }

        if (context.isAssistantExecuteResult()) {
            handleAssistantExecuteResult(context, payload, topic);
            return;
        }

        log.warn("MQTT inbound ignored: unsupported topic pattern={}", topic);
    }

    private TopicContext parseTopic(String topic) {
        if (topic == null || topic.isBlank()) {
            return null;
        }
        String[] parts = topic.split("/");
        if (!PREFIX.equalsIgnoreCase(parts[0])) {
            return null;
        }

        if (parts.length == 5 && DEVICE_SEGMENT.equalsIgnoreCase(parts[1]) && DATA_SEGMENT.equalsIgnoreCase(parts[4])) {
            String deviceId = parts[2];
            String domain = parts[3];
            if (deviceId == null || deviceId.isBlank() || domain == null || domain.isBlank()) {
                return null;
            }
            return TopicContext.sensor(deviceId, domain);
        }

        if (parts.length == 5
                && ASSISTANT_SEGMENT.equalsIgnoreCase(parts[2])
                && EXECUTE_SEGMENT.equalsIgnoreCase(parts[3])
                && RES_SEGMENT.equalsIgnoreCase(parts[4])) {
            String hoId = parts[1];
            if (hoId == null || hoId.isBlank()) {
                return null;
            }
            return TopicContext.assistantResult(hoId);
        }

        return null;
    }

    private SafetySensorInboundPayload parseSafetyPayload(String payload) {
        try {
            return objectMapper.readValue(payload, SafetySensorInboundPayload.class);
        } catch (JsonProcessingException e) {
            log.warn("MQTT inbound payload parse failed: {}", e.getMessage());
            return null;
        }
    }

    private void handleSensorData(TopicContext context, String payload, String topic) {
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

        log.warn("MQTT inbound ignored: unsupported sensor domain={} topic={}", context.domain(), topic);
    }

    private void handleAssistantExecuteResult(TopicContext context, String payload, String topic) {
        if (payload == null || payload.isBlank()) {
            log.warn("MQTT assistant ignored: empty payload topic={}", topic);
            return;
        }
        log.info("MQTT assistant execute result received hoId={} topic={}", context.hoId(), topic);
    }

    private record TopicContext(String deviceId, String domain, String hoId, boolean sensorData, boolean assistantExecuteResult) {
        static TopicContext sensor(String deviceId, String domain) {
            return new TopicContext(deviceId, domain, null, true, false);
        }

        static TopicContext assistantResult(String hoId) {
            return new TopicContext(null, null, hoId, false, true);
        }

        boolean isSensorData() {
            return sensorData;
        }

        boolean isAssistantExecuteResult() {
            return assistantExecuteResult;
        }
    }
}
