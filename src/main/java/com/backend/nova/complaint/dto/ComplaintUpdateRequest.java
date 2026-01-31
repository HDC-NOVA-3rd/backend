package com.backend.nova.complaint.dto;

import com.backend.nova.complaint.entity.ComplaintType;
import lombok.Builder;

@Builder
public record ComplaintUpdateRequest(
        ComplaintType type,
        String title,
        String content
) {}
