package com.backend.nova.mqtt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MqttRouterService {
    // 각 로직을 담당하는 핸들러들을 이곳에 주입
    private final MqttSafetyInboundHandler safetyHandler;
    private final MqttEnvInboundHandler envHandler;
    private final MqttEnvSaveHandler envSaveHandler;
    private final MqttAssistantInboundHandler assistantHandler;
    private final MqttVoiceInboundHandler voiceHandler;
    private final MqttEntranceInbound entranceInbound;

    // 모든 MQTT SUB 메시지를 라우팅하여 해당 핸들러에 전달
    @ServiceActivator(inputChannel = "mqttInputChannel")
    public void handleMessage(Message<String> message) {

        String topic = (String) message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC);

        if (topic == null) return;

        // 토픽에 포함된 문자열로 분기 (if-else or switch pattern matching)
        try {
            // 화재/안전 센서 데이터
            if (topic.contains("/safety/")) {
                safetyHandler.handleSafetyMessage(message);
            }
            // 환경 센서 데이터 (로깅 + DB 저장 두 가지 작업 수행)
            else if (topic.contains("/env/")) {
                envHandler.handleEnvMessage(message); // 로그용
                envSaveHandler.save(message);         // DB 저장용
            }
            // 챗봇 명령 실행 결과
            else if (topic.contains("/assistant/execute/res")) {
                assistantHandler.handleMessage(message);
            }
            // 음성 명령 요청
            else if (topic.contains("/assistant/voice/req")) {
                voiceHandler.handleMessage(message);
            }
            // 출입 QR 인증
            else if (topic.contains("/entrance/verify")) {
                entranceInbound.handleVerification(message);
            } else {
                log.warn("핸들링되지 않은 토픽입니다: {}", topic);
            }
        } catch (Exception e) {
            log.error("MQTT 메시지 처리 중 오류 발생: {}", e.getMessage(), e);
        }
    }
}
