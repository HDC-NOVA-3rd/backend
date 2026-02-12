package com.backend.nova.mqtt;

import com.backend.nova.homeEnvironment.entity.Room;
import com.backend.nova.homeEnvironment.entity.RoomEnvLog;
import com.backend.nova.homeEnvironment.repository.RoomEnvLogRepository;
import com.backend.nova.homeEnvironment.repository.RoomRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class MqttEnvSaveHandler{

    private final ObjectMapper objectMapper;
    private final RoomRepository roomRepository;
    private final RoomEnvLogRepository roomEnvLogRepository;

    @Transactional
    public void save(Message<String> message) {
        String payload = message.getPayload();

        try {
            EnvPayload p = objectMapper.readValue(payload, EnvPayload.class);

            if (p.roomId == null || p.sensorType == null || p.value == null) return;

            Room room = roomRepository.findById(p.roomId).orElseThrow();

            RoomEnvLog entity = RoomEnvLog.builder()
                    .room(room)
                    .sensorType(p.sensorType)
                    .sensorValue(p.value)
                    .unit(p.unit == null ? "" : p.unit)
                    .recordedAt(LocalDateTime.now())
                    .build();

            roomEnvLogRepository.save(entity);

            log.info(" room_env_log saved: roomId={}, sensorType={}, value={}", p.roomId, p.sensorType, p.value);
        } catch (Exception e) {
            log.warn("env save failed: {}", e.getMessage());
        }
    }

    public static class EnvPayload {
        public Long roomId;
        public String sensorType;
        public Integer value;
        public String unit;
        public String ts;
    }
}
