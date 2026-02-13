package com.backend.nova.complaint.repository;

import com.backend.nova.complaint.entity.ComplaintReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ComplaintReviewRepository extends JpaRepository<ComplaintReview, Long> {

    // 민원당 피드백 1개만 허용할 경우
    Optional<ComplaintReview> findByComplaint_Id(Long complaintId);
}
