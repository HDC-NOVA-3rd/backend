package com.backend.nova.complaint.repository;

import com.backend.nova.complaint.entity.ComplaintAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComplaintAnswerRepository extends JpaRepository<ComplaintAnswer, Long> {

    List<ComplaintAnswer> findByComplaint_IdOrderByCreatedAtAsc(Long complaintId);
}
