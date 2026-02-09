package com.backend.nova.management.dto;

import com.backend.nova.management.entity.ManagementFee;

import java.time.LocalDateTime;

public record ManagementFeeResponse(
        Long id,
        Long apartmentId,
        String name,
        String description,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ManagementFeeResponse from(ManagementFee entity) {
        return new ManagementFeeResponse(
                entity.getId(),
                entity.getApartment().getId(),
                entity.getName(),
                entity.getDescription(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
