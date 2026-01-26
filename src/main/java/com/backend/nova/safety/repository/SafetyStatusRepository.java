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
     * 아파트별 안전 상태 조회 (최신순)
     */
    List<SafetyStatusEntity> findByApartmentOrderByUpdatedAtDesc(Apartment apartment);

    /**
     * 아파트 ID로 안전 상태 조회 (최신순)
     */
    List<SafetyStatusEntity> findByApartmentIdOrderByUpdatedAtDesc(Long apartmentId);

    /**
     * 특정 구역의 안전 상태 조회
     */
    Optional<SafetyStatusEntity> findByApartmentIdAndArea(Long apartmentId, String area);

    /**
     * 아파트별 위험 상태인 구역 조회
     */
    List<SafetyStatusEntity> findByApartmentIdAndSafetyStatus(Long apartmentId, SafetyStatus safetyStatus);
}
