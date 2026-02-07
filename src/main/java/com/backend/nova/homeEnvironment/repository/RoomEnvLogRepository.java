package com.backend.nova.homeEnvironment.repository;

import com.backend.nova.homeEnvironment.entity.RoomEnvLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoomEnvLogRepository extends JpaRepository<RoomEnvLog, Long> {

    // 특정 방(roomId)의 특정 센서(sensorType) 최신 1건
    Optional<RoomEnvLog> findFirstByRoom_IdAndSensorTypeOrderByRecordedAtDesc(
            Long roomId,
            String sensorType
    );
}