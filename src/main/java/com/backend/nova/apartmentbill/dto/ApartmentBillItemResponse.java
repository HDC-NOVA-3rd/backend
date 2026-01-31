package com.backend.nova.apartmentbill.dto;

import com.backend.nova.apartmentbill.entity.ApartmentBillItem;

import java.time.LocalDateTime;

public record ApartmentBillItemResponse(
        Long id,
        Long apartmentId,
        String name,
        String description,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ApartmentBillItemResponse from(ApartmentBillItem entity) {
        return new ApartmentBillItemResponse(
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
