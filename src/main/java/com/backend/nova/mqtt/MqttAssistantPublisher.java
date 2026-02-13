package com.backend.nova.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MqttAssistantPublisher {

    private final MessageChannel mqttAssistantOutboundChannel;
    private final ObjectMapper objectMapper;

    @Value("${spring.mqtt.topic.assistant-req}")
    private String assistantReqTopicTemplate; // "hdc/{hoId}/assistant/execute/req"

    public void publishCommand(Long hoId, String deviceCode, String command, Object value) {
        String topic = assistantReqTopicTemplate.replace("{hoId}", String.valueOf(hoId));

        Map<String, Object> payload = Map.of(
                "traceId", UUID.randomUUID().toString(),
                "deviceCode", deviceCode,
                "command", command,
                "value", value
        );

        try {
            String json = objectMapper.writeValueAsString(payload);

            Message<String> message = MessageBuilder
                    .withPayload(json)
                    .setHeader(MqttHeaders.TOPIC, topic)
                    .build();

            mqttAssistantOutboundChannel.send(message);

        } catch (Exception e) {
            throw new RuntimeException("MQTT assistant publish 실패", e);
        }
    }
}
