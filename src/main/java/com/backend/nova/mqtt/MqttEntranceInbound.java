package com.backend.nova.mqtt;

import com.backend.nova.reservation.service.ReservationService;
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
public class MqttEntranceInbound {
    private final ObjectMapper objectMapper;
    private final ReservationService reservationService;
    private final MqttEntranceOutbound mqttEntranceOutbound;

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
            boolean isVerified = reservationService.verifyAndNotify(req.spaceId(), req.qrToken());

            // 3. 결과에 따른 처리
            if (isVerified) {
                log.info("Access GRANTED for spaceId={}, token={}", req.spaceId(), req.qrToken());
                mqttEntranceOutbound.sendOpenCommand(req.spaceId());
            } else {
                log.warn("Access DENIED for spaceId={}, token={}", req.spaceId(), req.qrToken());
                mqttEntranceOutbound.sendFailCommand(req.spaceId());
            }
        } catch (Exception e) {
            log.error("Entrance verification failed", e);
        }
    }
}
