package com.backend.nova.complaint.repository;

import com.backend.nova.complaint.entity.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    // ── 기본 조회 ──
    default Optional<Complaint> findActiveById(Long id) {
        return findByIdAndDeletedFalse(id);
    }

    // ── 리스트 조회 ──
    default List<Complaint> findAllActive() {
        return findByDeletedFalse();
    }

    // ID 기준 조회 → Optional로 반환
    Optional<Complaint> findByIdAndDeletedFalse(Long id);

    // 리스트 조회
    List<Complaint> findByDeletedFalse();
    List<Complaint> findByMember_IdAndDeletedFalse(Long memberId);
    List<Complaint> findByMember_Resident_Ho_Dong_Apartment_IdAndDeletedFalse(Long apartmentId);

    // 삭제된 민원 조회 (슈퍼 관리자)
    List<Complaint> findByDeletedTrue();
    List<Complaint> findByDeletedTrueAndApartment_Id(Long apartmentId);

    Optional<Complaint> findByIdAndMember_IdAndDeletedFalse(Long id, Long memberId);

    // 명칭을 더 명확하게 변경 (deleted 조건을 쿼리에서 뺌)
    List<Complaint> findByApartmentId(Long apartmentId);

    // deleted 상태에 따라 필터링하고 싶을 때 사용
    List<Complaint> findByApartmentIdAndDeleted(Long apartmentId, boolean deleted);

    List<Complaint> findByMemberIdAndDeletedFalse(Long memberId);

    List<Complaint> findByMemberIdAndDeletedTrue(Long memberId);

}


