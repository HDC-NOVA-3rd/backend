package com.backend.nova.complaint.dto;


import com.backend.nova.complaint.entity.Complaint;
import com.backend.nova.complaint.entity.ComplaintStatus;
import com.backend.nova.complaint.entity.ComplaintType;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ComplaintDetailResponse(
        Long complaintId,
        ComplaintType type,
        ComplaintStatus status,
        String title,
        String content,
        Long memberId,
        Long adminId,
        String adminName,
        Long apartmentId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,

        // --- 상세 페이지를 위해 추가될 필드 ---
        ComplaintAnswerResponse answer, // 관리자 답변 객체
        boolean hasReview,              // 리뷰 작성 여부
        LocalDateTime resolvedAt        // 해결 시간 (필요 시)
) {
    public static ComplaintDetailResponse of(Complaint complaint, boolean hasReview, ComplaintAnswerResponse answer) {
        return ComplaintDetailResponse.builder()
                .complaintId(complaint.getId())
                .type(complaint.getType())
                .status(complaint.getStatus())
                .title(complaint.getTitle())
                .content(complaint.getContent())
                .memberId(complaint.getMember().getId())
                .adminId(complaint.getAdmin() != null ? complaint.getAdmin().getId() : null)
                .adminName(complaint.getAdmin() != null ? complaint.getAdmin().getName() : "미지정")
                .apartmentId(complaint.getApartment().getId())
                .createdAt(complaint.getCreatedAt())
                .updatedAt(complaint.getUpdatedAt())
                .answer(answer)
                .hasReview(hasReview)
                .resolvedAt(complaint.getResolvedAt())
                .build();
    }
}

