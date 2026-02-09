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
public class MqttEnvInboundHandler {

    @ServiceActivator(inputChannel = "mqttEnvInputChannel")
    public void handleEnvMessage(Message<String> message) {
        String payload = message.getPayload();
        String topic = (String) message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC);

        log.info("MQTT env inbound topic={}, payload={}", topic, payload);

        String deviceId = extractDeviceId(topic);
        if (deviceId == null) {
            log.warn("MQTT env inbound ignored: invalid topic={}", topic);
            return;
        }

        if (payload == null || payload.isBlank()) {
            log.warn("MQTT env ignored: empty payload topic={}", topic);
            return;
        }
        log.info("MQTT env received deviceId={} topic={}", deviceId, topic);
    }

    private String extractDeviceId(String topic) {
        String[] parts = topic.split("/");
        String deviceId = parts[2];
        return deviceId == null || deviceId.isBlank() ? null : deviceId;
    }
}
