package com.backend.nova.homeEnvironment.repository;

import com.backend.nova.homeEnvironment.entity.RoomEnvLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomEnvLogRepository extends JpaRepository<RoomEnvLog, Long> {



    List<RoomEnvLog> findByRoom_IdAndSensorTypeOrderByRecordedAtDesc(
            Long roomId,
            String sensorType,
            Pageable pageable
    );

    Optional<Object> findTop1ByRoomId_IdAndSensorTypeOrderByRecordedAtDesc(Long id, String sensorType);
}