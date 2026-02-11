package com.backend.nova.facility.repository;

import com.backend.nova.facility.entity.Facility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FacilityRepository extends JpaRepository<Facility, Long> {

    /**
     * 아파트별 시설 목록 조회
     * GET /api/facility/{id}
     */
    // 일반 JOIN이 아닌 'JOIN FETCH'를 사용 -> 연관된 엔티티 즉시 로딩
    // DISTINCT: 1:N 조인 시 데이터 뻥튀기 방지
    @Query("SELECT DISTINCT f FROM Facility f " +
            "LEFT JOIN FETCH f.spaces " +
            "LEFT JOIN FETCH f.images " +
            "WHERE f.apartment.id = :apartmentId")
    List<Facility> findAllByApartmentId(Long apartmentId);

    /**
     * 아파트 ID와 시설 ID 챗봇용 메서드
     */
    Optional<Facility> findByApartmentIdAndName(Long apartmentId, String name);

    /**
     * 시설 ID로 상세 조회 (이미지까지 같이 받아옴)
     */
    @Query("SELECT DISTINCT f FROM Facility f " +
            "LEFT JOIN FETCH f.images " +
            "WHERE f.id = :facilityId")
    Optional<Facility> findByIdWithImages(Long facilityId);
}
