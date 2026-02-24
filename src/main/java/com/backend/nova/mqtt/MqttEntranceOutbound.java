package com.backend.nova.mqtt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class MqttEntranceOutbound {
    private final MessageChannel mqttOutboundChannel;

    // 1. 스캔 시작 명령 전송
    public void sendScanCommand(String spaceId) {
        String topic = String.format("hdc/entrance/scan/%s", spaceId);
        String payload = "{\"command\": \"scan start\"}";
        send(topic, payload);
        log.info("Sent SCAN_START to {}", topic);
    }

    // 2. 문 열기 명령 전송
    public void sendOpenCommand(String spaceId) {
        String topic = String.format("hdc/entrance/command/%s", spaceId);
        String payload = "{\"command\": \"OPEN_DOOR\"}";
        send(topic, payload);
        log.info("Sent OPEN_DOOR to {}", topic);
    }
    // 2. 문 열기 실패 전송
    public void sendFailCommand(String spaceId) {
        String topic = String.format("hdc/entrance/command/%s", spaceId);
        String payload = "{\"command\": \"FAIL_DOOR\"}";
        send(topic, payload);
        log.info("Sent FAIL_DOOR to {}", topic);
    }

    // 공통 전송 로직
    private void send(String topic, String payload) {
        try {
            Message<String> message = MessageBuilder
                    .withPayload(payload)
                    .setHeader(MqttHeaders.TOPIC, topic)
                    .build();
            mqttOutboundChannel.send(message);
        } catch (Exception e) {
            log.error("MQTT send failed: topic={}", topic, e);
            throw new RuntimeException("기기 통신 오류");
        }
    }
}
