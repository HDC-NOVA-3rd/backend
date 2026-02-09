package com.backend.nova.facility.repository;

import com.backend.nova.facility.entity.Space;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpaceRepository extends JpaRepository<Space, Long> {


    /**
     * 시설별 공간 목록 조회
     * GET /api/facility/{facilityId}/space
     */
    List<Space> findAllByFacilityId(Long facilityId);

    /**
     * 인원 수를 기반으로 예약 가능한 공간 조회
     * GET /api/facility/{facilityId}/space?capacity={}
     */
    @Query("SELECT s FROM Space s " +
            "WHERE s.facility.id = :facilityId " +
            "AND :reqCapacity BETWEEN s.minCapacity AND s.maxCapacity")
    List<Space> findSpacesByCapacity(
            @Param("facilityId") Long facilityId,
            @Param("reqCapacity") Integer reqCapacity
    );

    // 시실 정보 조회
    Optional<Space> findByFacility_IdAndName(Long facilityId, String name);
}
