package com.backend.nova.safety.repository;

import com.backend.nova.safety.entity.SensorLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SensorLogRepository extends JpaRepository<SensorLog, Long> {

    /**
     * 센서별 로그 조회 (최신순 - ID 역순)
     */
    List<SensorLog> findBySensorIdOrderByIdDesc(Long sensorId);

    /**
     * 센서별 최근 N개 로그 조회
     */
    List<SensorLog> findTop10BySensorIdOrderByIdDesc(Long sensorId);
}
