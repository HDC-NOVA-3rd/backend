package com.backend.nova.complaint.dto;

import com.backend.nova.complaint.entity.Complaint;
import com.backend.nova.complaint.entity.ComplaintStatus;
import com.backend.nova.complaint.entity.ComplaintType;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ComplaintResponse(
        Long id,
        ComplaintType type,
        ComplaintStatus status,
        String title,
        String content,
        Long memberId,
        Long adminId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ComplaintResponse from(Complaint complaint) {
        return ComplaintResponse.builder()
                .id(complaint.getId())
                .type(complaint.getType())
                .status(complaint.getStatus())
                .title(complaint.getTitle())
                .content(complaint.getContent())
                .memberId(complaint.getMember().getId())
                .adminId(complaint.getAdmin() != null ? complaint.getAdmin().getId() : null)
                .createdAt(complaint.getCreatedAt())
                .updatedAt(complaint.getUpdatedAt())
                .build();
    }
}
