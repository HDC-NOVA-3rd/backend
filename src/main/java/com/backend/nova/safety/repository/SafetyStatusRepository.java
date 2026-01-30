package com.backend.nova.safety.repository;

import com.backend.nova.apartment.entity.Apartment;
import com.backend.nova.safety.entity.SafetyStatusEntity;
import com.backend.nova.safety.enums.SafetyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SafetyStatusRepository extends JpaRepository<SafetyStatusEntity, Long> {
    /**
     * 아파트 ID로 안전 상태 조회 (최신순)
     */
    List<SafetyStatusEntity> findByApartmentIdOrderByUpdatedAtDesc(Long apartmentId);
    /**
     * 특정 구역(FACILITY)의 안전 상태 조회
     */
    Optional<SafetyStatusEntity> findByApartmentIdAndFacilityId(Long apartmentId, Long facilityId);

    /**
     * 특정 구역(DONG)의 안전 상태 조회
     */
    Optional<SafetyStatusEntity> findByApartmentIdAndDongId(Long apartmentId, Long dongId);
}
