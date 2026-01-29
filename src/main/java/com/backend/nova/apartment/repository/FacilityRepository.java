package com.backend.nova.apartment.repository;

import com.backend.nova.apartment.entity.Facility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FacilityRepository extends JpaRepository<Facility, Long> {

    /**
     * 아파트별 시설 목록 조회
     * GET /api/facility/{id}
     */
    List<Facility> findByApartmentId(Long apartmentId);

    /**
     * 아파트 ID와 시설 ID로 상세 조회
     */
    Facility findByIdAndApartmentId(Long facilityId, Long apartmentId);


    /**
     * 아파트 ID와 시설 ID로 상세 조회 챗봇용 메서드
     */
    Optional<Facility> findByApartmentIdAndName(Long apartmentId, String name);
}
