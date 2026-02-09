package com.backend.nova.homeEnvironment.service;

import com.backend.nova.homeEnvironment.dto.DeviceSnapshot;
import com.backend.nova.homeEnvironment.dto.RoomSnapshotResponse;
import com.backend.nova.homeEnvironment.entity.Room;
import com.backend.nova.homeEnvironment.entity.RoomEnvLog;
import com.backend.nova.homeEnvironment.repository.DeviceRepository;
import com.backend.nova.homeEnvironment.repository.RoomEnvLogRepository;
import com.backend.nova.homeEnvironment.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomSnapshotService {

    private final RoomRepository roomRepository;
    private final DeviceRepository deviceRepository;
    private final RoomEnvLogRepository roomEnvLogRepository;

    // RoomEnvLog에 저장할 때 sensorType 문자열이 뭐였는지에 따라 여기 값을 맞춰야 함
    private static final String SENSOR_TEMP = "TEMP";
    private static final String SENSOR_HUMI = "HUMIDITY";

    // 제어 정보 불러오기
    public RoomSnapshotResponse getSnapshot(Long roomId) {

        // 1) 방 정보
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("방을 찾을 수 없습니다. roomId=" + roomId));

        // 2) 최신 온습도(없을 수도 있으니까 null 허용)
        Integer temperature = getLatestValue(roomId, SENSOR_TEMP);
        Integer humidity = getLatestValue(roomId, SENSOR_HUMI);

        // 3) 방에 있는 디바이스 목록
        List<DeviceSnapshot> device = deviceRepository.findAllByRoom_Id(roomId)
                .stream()
                .map(DeviceSnapshot::from)
                .toList();

        // 4) 합쳐서 응답
        return new RoomSnapshotResponse(
                room.getId(),
                room.getName(),
                temperature,
                humidity,
                device
        );
    }

    // 온습도 값 불러오기
    private Integer getLatestValue(Long roomId, String sensorType) {
        return roomEnvLogRepository
                .findFirstByRoom_IdAndSensorTypeOrderByRecordedAtDesc(roomId, sensorType)
                .map(RoomEnvLog::getSensorValue)
                .orElse(null);
    }
}
