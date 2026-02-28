package com.backend.nova.complaint.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record ComplaintReviewCreateRequest(
        String content,
        BigDecimal rating
) {}
