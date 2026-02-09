package com.backend.nova.complaint.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record ComplaintFeedbackCreateRequest(
        String content,
        BigDecimal rating
) {}
