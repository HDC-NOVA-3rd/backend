package com.backend.nova.resident.dto;

import com.backend.nova.resident.entity.Resident;

public record ResidentResponse(
        Long residentId,
        String apartmentName,
        String dongNo,
        String hoNo,
        Long hoId,
        String name,
        String phone
) {
    public static ResidentResponse from(Resident resident) {
        return new ResidentResponse(
                resident.getId(),
                resident.getHo().getDong().getApartment().getName(),
                resident.getHo().getDong().getDongNo(),
                resident.getHo().getHoNo(),
                resident.getHo().getId(),
                resident.getName(),
                resident.getPhone()
        );
    }
}