package com.backend.nova.mqtt;

import com.backend.nova.reservation.service.ReservationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class MqttEntranceHandler {
    private final ObjectMapper objectMapper;
    private final ReservationService reservationService;
    private final MessageChannel mqttOutboundChannel;

    private record EntranceVerifyRequest(
            String spaceId,
            String qrToken,
            Double timestamp // 또는 Long
    ) {}

    // 인증 QR과 spaceId 을 reservation에 전달
    public void handleVerification(Message<String> message){
        String payload = message.getPayload();
        String topic = (String) message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC);

        log.info("MQTT verify topic={}", topic);
        try{
            // 1. 파싱
            EntranceVerifyRequest req = objectMapper.readValue(payload, EntranceVerifyRequest.class);

            if (req.spaceId() == null || req.qrToken() == null) {
                log.warn("Invalid entrance payload: {}", payload);
                return;
            }

            // 2. 서비스 호출 (검증 & 로그 저장)
            boolean isVerified = true;
                    //reservationService.verifyAndLogEntrance(req.getSpaceId(), req.getQrToken());

            // 3. 결과에 따른 처리
            if (isVerified) {
                log.info("Access GRANTED for spaceId={}, token={}", req.spaceId(), req.qrToken());
                sendOpenCommand(req.spaceId());
            } else {
                log.warn("Access DENIED for spaceId={}, token={}", req.spaceId(), req.qrToken());
                // 필요하다면 '실패 알림' 메시지를 보낼 수도 있음
                // sendErrorCommand(req.getSpaceId(), "Invalid Token");
            }
        } catch (Exception e) {
            log.error("Entrance verification failed", e);
        }
    }

    private void sendOpenCommand(String spaceId) {
        // 라즈베리파이가 구독할 토픽: hdc/entrance/{spaceId}/command
        String commandTopic = String.format("hdc/entrance/command/%s", spaceId);

        // 보낼 메시지 (라즈베리파이 Python 코드의 EntranceWorker가 기대하는 포맷)
        // {"command": "OPEN_DOOR", "traceId": "..."}
        String commandPayload = "{\"command\": \"OPEN_DOOR\"}";

        try {
            Message<String> outMsg = MessageBuilder
                    .withPayload(commandPayload)
                    .setHeader(MqttHeaders.TOPIC, commandTopic)
                    .build();

            mqttOutboundChannel.send(outMsg);
            log.info("Sent OPEN command to {}", commandTopic);

        } catch (Exception e) {
            log.error("Failed to send OPEN command", e);
        }
    }
}
