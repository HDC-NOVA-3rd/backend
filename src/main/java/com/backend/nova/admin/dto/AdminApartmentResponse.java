package com.backend.nova.admin.dto;

import com.backend.nova.apartment.entity.Apartment;

public record AdminApartmentResponse(
        Long apartmentId,
        String apartmentName,
        String address
) {
    public static AdminApartmentResponse from(Apartment apartment) {
        return null;
    }
}
