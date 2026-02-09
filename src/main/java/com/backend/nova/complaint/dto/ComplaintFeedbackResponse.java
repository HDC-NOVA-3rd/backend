package com.backend.nova.complaint.dto;

import com.backend.nova.complaint.entity.ComplaintFeedback;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record ComplaintFeedbackResponse(
        Long id,
        Long memberId,
        String content,
        BigDecimal rating,
        LocalDateTime createdAt
) {
    public static ComplaintFeedbackResponse from(ComplaintFeedback feedback) {
        return ComplaintFeedbackResponse.builder()
                .id(feedback.getId())
                .memberId(feedback.getMember().getId())
                .content(feedback.getContent())
                .rating(feedback.getRating())
                .createdAt(feedback.getCreatedAt())
                .build();
    }
}
