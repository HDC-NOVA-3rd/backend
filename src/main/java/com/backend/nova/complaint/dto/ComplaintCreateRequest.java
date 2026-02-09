package com.backend.nova.complaint.dto;

import com.backend.nova.complaint.entity.ComplaintType;
import lombok.Builder;

@Builder
public record ComplaintCreateRequest(
        ComplaintType type,
        String title,
        String content
) { }
