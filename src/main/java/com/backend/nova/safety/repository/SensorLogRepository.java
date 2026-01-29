package com.backend.nova.safety.repository;

import com.backend.nova.safety.entity.SafetySensorLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SensorLogRepository extends JpaRepository<SafetySensorLog, Long> {

    /**
     * 아파트별 센서 로그 조회 (최신순 - ID 역순)
     */
    List<SafetySensorLog> findBySafetySensor_Apartment_IdOrderByIdDesc(Long apartmentId);
}
