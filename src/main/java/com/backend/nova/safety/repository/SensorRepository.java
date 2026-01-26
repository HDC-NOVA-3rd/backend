package com.backend.nova.safety.repository;

import com.backend.nova.safety.entity.Sensor;
import com.backend.nova.safety.enums.SensorType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SensorRepository extends JpaRepository<Sensor, Long> {

    /**
     * 센서 이름으로 조회
     */
    Optional<Sensor> findByName(String name);

    /**
     * 호별 센서 조회
     */
    List<Sensor> findByHoId(Long hoId);

    /**
     * 공간별 센서 조회
     */
    List<Sensor> findBySpaceId(Long spaceId);

    /**
     * 센서 타입별 조회
     */
    List<Sensor> findBySensorType(SensorType sensorType);
}
