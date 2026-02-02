package com.backend.nova.mqtt;

import com.backend.nova.safety.dto.SafetySensorInboundPayload;
import com.backend.nova.safety.service.SafetyService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MqttInboundHandlerTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private SafetyService safetyService;

    @InjectMocks
    private MqttSafetyInboundHandler mqttInboundHandler;

    @Test
    void ignoresInvalidTopic() {
        // topic 패턴 불일치면 무시
        Message<String> message = message("hdc/device/s1/safety", "{\"deviceId\":\"s1\"}");
        mqttInboundHandler.handleSafetyMessage(message);
        verifyNoInteractions(objectMapper, safetyService);
    }

    @Test
    void ignoresInvalidPayload() throws Exception {
        // 필수 필드 누락이면 서비스 호출하지 않음
        Message<String> message = message("hdc/device/s1/safety/data", "{\"deviceId\":\"s1\"}");
        when(objectMapper.readValue(eq(message.getPayload()), eq(SafetySensorInboundPayload.class)))
                .thenReturn(new SafetySensorInboundPayload(null, 1.0, "ppm", "2024-01-01T00:00:00Z"));

        mqttInboundHandler.handleSafetyMessage(message);

        verify(safetyService, never()).handleSafetySensor(anyString(), any());
    }

    @Test
    void ignoresPayloadWhenMissingUnit() throws Exception {
        // 필수 필드 누락이면 서비스 호출하지 않음
        Message<String> message = message("hdc/device/s1/safety/data", "{\"deviceId\":\"s1\"}");
        when(objectMapper.readValue(eq(message.getPayload()), eq(SafetySensorInboundPayload.class)))
                .thenReturn(new SafetySensorInboundPayload("SMOKE", 1.0, null, "2024-01-01T00:00:00Z"));

        mqttInboundHandler.handleSafetyMessage(message);

        verify(safetyService, never()).handleSafetySensor(anyString(), any());
    }

    @Test
    void routesValidMessageToService() throws Exception {
        // 정상 메시지는 서비스로 전달
        Message<String> message = message("hdc/device/s1/safety/data", "{\"deviceId\":\"s1\"}");
        SafetySensorInboundPayload payload = new SafetySensorInboundPayload(
                "SMOKE",
                10.0,
                "ppm",
                "2024-01-01T00:00:00Z"
        );
        when(objectMapper.readValue(eq(message.getPayload()), eq(SafetySensorInboundPayload.class)))
                .thenReturn(payload);

        mqttInboundHandler.handleSafetyMessage(message);

        ArgumentCaptor<SafetySensorInboundPayload> captor = ArgumentCaptor.forClass(SafetySensorInboundPayload.class);
        verify(safetyService).handleSafetySensor(eq("s1"), captor.capture());
        assertThat(captor.getValue()).isEqualTo(payload);
    }

    @Test
    void ignoresWhenPayloadParseFails() throws Exception {
        // JSON 파싱 실패 시 무시
        Message<String> message = message("hdc/device/s1/safety/data", "{\"deviceId\":\"s1\"}");
        when(objectMapper.readValue(eq(message.getPayload()), eq(SafetySensorInboundPayload.class)))
                .thenThrow(new JsonProcessingException("boom") {});

        mqttInboundHandler.handleSafetyMessage(message);

        verify(safetyService, never()).handleSafetySensor(anyString(), any());
    }

    private Message<String> message(String topic, String payload) {
        return MessageBuilder.withPayload(payload)
                .setHeader(MqttHeaders.RECEIVED_TOPIC, topic)
                .build();
    }
}
