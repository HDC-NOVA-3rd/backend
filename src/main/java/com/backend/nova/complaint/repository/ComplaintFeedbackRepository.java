package com.backend.nova.complaint.repository;

import com.backend.nova.complaint.entity.ComplaintFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ComplaintFeedbackRepository extends JpaRepository<ComplaintFeedback, Long> {

    // 민원당 피드백 1개만 허용할 경우
    Optional<ComplaintFeedback> findByComplaint_Id(Long complaintId);
}
