package com.backend.nova.admin.dto;

public record AdminApartmentResponse(
        Long apartmentId,
        String apartmentName,
        String address
) {
}
