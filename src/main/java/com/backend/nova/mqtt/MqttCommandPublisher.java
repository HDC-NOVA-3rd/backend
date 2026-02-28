package com.backend.nova.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class MqttCommandPublisher {

    private final MessageChannel mqttOutboundChannel;
    private final ObjectMapper objectMapper;

    public MqttCommandPublisher(
            @Qualifier("mqttOutboundChannel") MessageChannel mqttOutboundChannel,
            ObjectMapper objectMapper
    ) {
        this.mqttOutboundChannel = mqttOutboundChannel;
        this.objectMapper = objectMapper;
    }

    public void publishDeviceCommand(
            Long hoId,
            Long roomId,
            String deviceCode,
            String command,
            Object value
    ) {
        String topic = "hdc/" + hoId + "/room/" + roomId + "/device/execute/req";

        Map<String, Object> payload = new HashMap<>();
        payload.put("traceId", UUID.randomUUID().toString());
        payload.put("roomId", roomId);
        payload.put("deviceCode", deviceCode);
        payload.put("command", command);
        payload.put("value", value);

        try {
            String json = objectMapper.writeValueAsString(payload);

            Message<String> message = MessageBuilder
                    .withPayload(json)
                    .setHeader(MqttHeaders.TOPIC, topic)
                    .build();

            mqttOutboundChannel.send(message);

        } catch (Exception e) {
            throw new RuntimeException("MQTT publish 실패", e);
        }
    }
}