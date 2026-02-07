package com.backend.nova.complaint.repository;

import com.backend.nova.complaint.entity.Complaint;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    // 멤버별 민원 조회
    List<Complaint> findByMember_Id(Long memberId);

    // 아파트별 민원 조회 (관리자)
    List<Complaint> findByMember_Resident_Ho_Dong_Apartment_Id(Long apartmentId);

    // 상세 조회 시 연관 엔티티 한 번에
    @EntityGraph(attributePaths = {
            "member",
            "member.resident",
            "admin",
            "answers",
            "feedbacks"
    })
    Complaint findWithAllById(Long id);
}
