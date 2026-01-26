package com.backend.nova.resident.dto;

import com.backend.nova.resident.entity.Resident;

public record ResidentResponseDto(
        Long residentId,
        String apartmentName,
        String dongNo,
        String hoNo,
        String name,
        String phone
) {
    public ResidentResponseDto(Resident resident) {
        this(
                resident.getId(),
                resident.getHo().getDong().getApartment().getName(),
                resident.getHo().getDong().getDongNo(),
                resident.getHo().getHoNo(),
                resident.getName(),
                resident.getPhone()
        );
    }
}