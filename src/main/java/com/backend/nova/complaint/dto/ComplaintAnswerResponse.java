package com.backend.nova.complaint.dto;

import com.backend.nova.complaint.entity.ComplaintAnswer;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ComplaintAnswerResponse(
        Long id,
        Long adminId,
        String resultContent,
        LocalDateTime createdAt
) {
    public static ComplaintAnswerResponse from(ComplaintAnswer answer) {
        return ComplaintAnswerResponse.builder()
                .id(answer.getId())
                .adminId(answer.getAdmin().getId())
                .resultContent(answer.getResultContent())
                .createdAt(answer.getCreatedAt())
                .build();
    }
}
