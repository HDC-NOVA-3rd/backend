package com.backend.nova.safety.repository;

import com.backend.nova.apartment.entity.Apartment;
import com.backend.nova.safety.entity.SafetyEventLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SafetyEventLogRepository extends JpaRepository<SafetyEventLog, Long> {

    /**
     * 아파트별 이벤트 로그 조회 (최신순)
     */
    List<SafetyEventLog> findByApartmentOrderByEventAtDesc(Apartment apartment);

    /**
     * 아파트 ID로 이벤트 로그 조회 (최신순)
     */
    List<SafetyEventLog> findByApartmentIdOrderByEventAtDesc(Long apartmentId);

    /**
     * 특정 구역(DONG)의 이벤트 로그 조회 (최신순)
     */
    List<SafetyEventLog> findByApartmentIdAndDongIdOrderByEventAtDesc(Long apartmentId, Long dongId);

    /**
     * 특정 구역(FACILITY)의 이벤트 로그 조회 (최신순)
     */
    List<SafetyEventLog> findByApartmentIdAndFacilityIdOrderByEventAtDesc(Long apartmentId, Long facilityId);

    /**
     * 특정 기간의 이벤트 로그 조회
     */
    List<SafetyEventLog> findByApartmentIdAndEventAtBetweenOrderByEventAtDesc(
            Long apartmentId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 센서별 이벤트 로그 조회 (최신순)
     */
    List<SafetyEventLog> findBySensorIdOrderByEventAtDesc(Long sensorId);
}
