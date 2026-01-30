package com.backend.nova.safety.repository;

import com.backend.nova.safety.entity.SafetySensorLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SensorLogRepository extends JpaRepository<SafetySensorLog, Long> {

    /**
     * 센서별 로그 조회 (최신순 - ID 역순)
     * 아파트별 센서 로그 조회 (최신순 - ID 역순)
     */
    List<SafetySensorLog> findBySafetySensor_Apartment_IdOrderByIdDesc(Long apartmentId);
    List<SensorLog> findBySensorIdOrderByIdDesc(Long sensorId);

    /**
     * 센서별 최근 N개 로그 조회
     */
    List<SensorLog> findTop10BySensorIdOrderByIdDesc(Long sensorId);
    List<SensorLog> findBySensor_Apartment_IdOrderByIdDesc(Long apartmentId);
}
