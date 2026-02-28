package com.backend.nova.safety.repository;

import com.backend.nova.safety.entity.SafetySensorLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SensorLogRepository extends JpaRepository<SafetySensorLog, Long> {

    /**
     * 아파트별 센서 로그 조회 (최신순 - ID 역순)
     */
    List<SafetySensorLog> findBySafetySensor_Apartment_IdOrderByIdDesc(Long apartmentId);

    /**
     * 센서별 최신 로그 1건씩만 조회 (대시보드 현황용)
     */
    @Query("SELECT sl FROM SafetySensorLog sl WHERE sl.id IN " +
           "(SELECT MAX(sl2.id) FROM SafetySensorLog sl2 WHERE sl2.safetySensor.apartment.id = :apartmentId GROUP BY sl2.safetySensor.id)")
    List<SafetySensorLog> findLatestPerSensorByApartmentId(@Param("apartmentId") Long apartmentId);

    /**
     * 특정 센서의 최신 로그 조회 (중복 방지 체크용)
     */
    Optional<SafetySensorLog> findTopBySafetySensorIdOrderByIdDesc(Long sensorId);
}
