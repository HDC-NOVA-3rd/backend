package com.backend.nova.homeEnvironment.service;

import com.backend.nova.homeEnvironment.dto.RoomEnvironmentResponse;
import com.backend.nova.homeEnvironment.entity.RoomEnvLog;
import com.backend.nova.homeEnvironment.repository.RoomEnvLogRepository;
import com.backend.nova.homeEnvironment.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HomeEnvironmentService {
    private final RoomRepository roomRepository;
    private final RoomEnvLogRepository roomEnvLogRepository;
    // roomId 기준: 해당 방의 최신 TEMP/HUMIDITY를 조회해서 응답 DTO로 만듦.
    @Transactional(readOnly = true)
    public RoomEnvironmentResponse getRoomEnvironment(Long roomId) {

        //Temp 최신 1개
        Double temperature = roomEnvLogRepository
                .findFirstByRoom_IdAndSensorTypeOrderByRecordedAtDesc(roomId, "TEMP")
                .map(RoomEnvLog::getSensorValue)
                .map(v -> v / 10.0)
                .orElse(null);
        // Humidity 최신 1개
        Integer humidity = roomEnvLogRepository
                .findFirstByRoom_IdAndSensorTypeOrderByRecordedAtDesc(roomId, "HUMIDITY")
                .map(RoomEnvLog::getSensorValue)
                .orElse(null);

        return RoomEnvironmentResponse.builder()
                .roomId(roomId)
                .temperature(temperature)
                .humidity(humidity)
                .build();
    }
    // hoId 기준: 해당 세대의 모든 방(room)을 조회한 뒤, 각 방의 환경 DTO를 만들어 리스트로 반환
    @Transactional(readOnly = true)
    public List<RoomEnvironmentResponse> getRoomEnvironments(Long hoId) {
        return roomRepository.findAllByHo_Id(hoId).stream()
                .map(room -> getRoomEnvironment(room.getId()))
                .toList();
    }
}
