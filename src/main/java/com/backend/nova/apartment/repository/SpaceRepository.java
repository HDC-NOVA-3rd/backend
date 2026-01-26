package com.backend.nova.apartment.repository;

import com.backend.nova.apartment.entity.Space;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpaceRepository extends JpaRepository<Space, Long> {

    /**
     * 시설별 공간 목록 조회
     * GET /api/facilities/{apartmentId}/{facilityId}
     */
    List<Space> findByFacilityId(Long facilityId);

    /**
     * 인원 수를 기반으로 예약 가능한 공간 조회
     * GET /api/facilities/{apartmentId}/{facilityId}?capacity={}
     */
    List<Space> findByFacilityIdAndMinCapacityLessThanEqualAndMaxCapacityGreaterThanEqual(
            Long facilityId, Integer capacity, Integer capacity2);
}
