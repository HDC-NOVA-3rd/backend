package com.backend.nova.complaint.dto;

import com.backend.nova.complaint.entity.ComplaintReview;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record ComplaintReviewResponse(
        Long id,
        Long memberId,
        String content,
        BigDecimal rating,
        LocalDateTime createdAt
) {
    public static ComplaintReviewResponse from(ComplaintReview feedback) {
        return ComplaintReviewResponse.builder()
                .id(feedback.getId())
                .memberId(feedback.getMember().getId())
                .content(feedback.getContent())
                .rating(feedback.getRating())
                .createdAt(feedback.getCreatedAt())
                .build();
    }
}
