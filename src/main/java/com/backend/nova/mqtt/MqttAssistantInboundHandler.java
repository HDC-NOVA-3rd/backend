package com.backend.nova.mqtt;

import com.backend.nova.chat.entity.DeviceCommandLog;
import com.backend.nova.chat.repository.DeviceCommandLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class MqttAssistantInboundHandler {

    private static final String PREFIX = "hdc";
    private static final String ASSISTANT_SEGMENT = "assistant";
    private static final String EXECUTE_SEGMENT = "execute";
    private static final String RES_SEGMENT = "res";

    //자바 객체 파싱
    private final ObjectMapper objectMapper;
    private final DeviceCommandLogRepository deviceCommandLogRepository;

    /**
     * hdc/{hoId}/assistant/execute/res
     * payload: {"traceId":"...","status":"SUCCESS","message":"LED ON"}
     */
    @Transactional
    public void handleMessage(Message<String> message) {
        String payload = message.getPayload();
        String topic = (String) message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC);

        log.info("MQTT assistant inbound topic={}, payload={}", topic, payload);

        String hoId = parseHoId(topic);
        if (hoId == null) {
            log.warn("MQTT assistant inbound ignored: invalid topic={}", topic);
            return;
        }
        //토픽 형식이 맞는지 확인하고, 브로커에서 이상값이 들어와도 서버가 터지지 않음
        if (payload == null || payload.isBlank()) {
            log.warn("MQTT assistant inbound ignored: empty payload topic={}", topic);
            return;
        }

        ExecuteResultRes res;
        try {
            res = objectMapper.readValue(payload, ExecuteResultRes.class);
        } catch (Exception e) {
            log.error("MQTT assistant inbound parse failed. topic={}, payload={}", topic, payload, e);
            return;
        }

        if (res.traceId == null || res.traceId.isBlank()) {
            log.warn("MQTT assistant inbound ignored: traceId missing. topic={}, payload={}", topic, payload);
            return;
        }
        //traceId로 DB 조회 & 상태 업데이트
        Optional<DeviceCommandLog> opt = deviceCommandLogRepository.findById(res.traceId);
        if (opt.isEmpty()) {
            log.warn("DeviceCommandLog not found for traceId={}", res.traceId);
            return;
        }

        DeviceCommandLog logEntity = opt.get();
        if ("SUCCESS".equalsIgnoreCase(res.status)) {
            logEntity.markSuccess();
        } else {
            logEntity.markFailed();
        }

        // @Transactional + dirty checking 이면 save 없어도 되지만, 명시적으로 호출해도 OK
        deviceCommandLogRepository.save(logEntity);

        log.info(
                "MQTT assistant execute result applied. hoId={}, traceId={}, status={}, message={}",
                hoId, res.traceId, res.status, res.message
        );
    }

    private String parseHoId(String topic) {
        if (topic == null || topic.isBlank()) return null;

        String[] parts = topic.split("/");
        // ["hdc", "{hoId}", "assistant", "execute", "res"]
        if (parts.length != 5 || !PREFIX.equalsIgnoreCase(parts[0])) return null;

        if (!ASSISTANT_SEGMENT.equalsIgnoreCase(parts[2])
                || !EXECUTE_SEGMENT.equalsIgnoreCase(parts[3])
                || !RES_SEGMENT.equalsIgnoreCase(parts[4])) {
            return null;
        }

        String hoId = parts[1];
        return (hoId == null || hoId.isBlank()) ? null : hoId;
    }

    @Data
    private static class ExecuteResultRes {
        private String traceId;
        private String status;   // SUCCESS / FAIL (혹은 FAILED)
        private String message;
    }
}
