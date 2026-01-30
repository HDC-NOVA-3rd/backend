package com.backend.nova.mqtt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class MqttAssistantInboundHandler {

    private static final String PREFIX = "hdc";
    private static final String ASSISTANT_SEGMENT = "assistant";
    private static final String EXECUTE_SEGMENT = "execute";
    private static final String RES_SEGMENT = "res";

    @ServiceActivator(inputChannel = "mqttAssistantInputChannel")
    public void handleMessage(Message<String> message) {
        String payload = message.getPayload();
        String topic = (String) message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC);

        log.info("MQTT assistant inbound topic={}, payload={}", topic, payload);

        String hoId = parseHoId(topic);
        if (hoId == null) {
            log.warn("MQTT assistant inbound ignored: invalid topic={}", topic);
            return;
        }

        if (payload == null || payload.isBlank()) {
            log.warn("MQTT assistant ignored: empty payload topic={}", topic);
            return;
        }

        log.info("MQTT assistant execute result received hoId={} topic={}", hoId, topic);
    }

    private String parseHoId(String topic) {
        if (topic == null || topic.isBlank()) {
            return null;
        }
        String[] parts = topic.split("/");
        if (parts.length != 5 || !PREFIX.equalsIgnoreCase(parts[0])) {
            return null;
        }
        if (!ASSISTANT_SEGMENT.equalsIgnoreCase(parts[2])
                || !EXECUTE_SEGMENT.equalsIgnoreCase(parts[3])
                || !RES_SEGMENT.equalsIgnoreCase(parts[4])) {
            return null;
        }
        String hoId = parts[1];
        if (hoId == null || hoId.isBlank()) {
            return null;
        }
        return hoId;
    }
}
