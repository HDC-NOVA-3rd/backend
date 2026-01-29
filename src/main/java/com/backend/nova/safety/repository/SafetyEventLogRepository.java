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
     * 아파트 ID로 이벤트 로그 조회 (최신순)
     */
    List<SafetyEventLog> findByApartmentIdOrderByEventedAtDesc(Long apartmentId);
}
