package com.backend.nova.homeEnvironment.service;

import com.backend.nova.homeEnvironment.dto.RoomEnvironmentResponse;
import com.backend.nova.homeEnvironment.entity.Room;
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

    // 특정 방 실내 조회
    @Transactional(readOnly = true)
    public RoomEnvironmentResponse getRoomEnvironment(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 roomId: " + roomId));

        return buildRoomEnvironmentResponse(room);
    }

    // 전체 방 실내 조회
    @Transactional(readOnly = true)
    public List<RoomEnvironmentResponse> getRoomEnvironments(Long hoId) {
        return roomRepository.findAllByHo_Id(hoId).stream()
                .map(this::buildRoomEnvironmentResponse)
                .toList();
    }

    // DTO 생성 공통 로직 (정수 그대로 + roomName 포함)
    private RoomEnvironmentResponse buildRoomEnvironmentResponse(Room room) {
        Long roomId = room.getId();

        // TEMP 최신 1개 (정수 그대로 사용, /10 변환 삭제)
        Integer temperature = roomEnvLogRepository
                .findFirstByRoom_IdAndSensorTypeOrderByRecordedAtDesc(roomId, "TEMP")
                .map(RoomEnvLog::getSensorValue)
                .orElse(null);

        // HUMIDITY 최신 1개
        Integer humidity = roomEnvLogRepository
                .findFirstByRoom_IdAndSensorTypeOrderByRecordedAtDesc(roomId, "HUMIDITY")
                .map(RoomEnvLog::getSensorValue)
                .orElse(null);

        return new RoomEnvironmentResponse(
                roomId,
                room.getName(),
                temperature,
                humidity
        );

    }
}
