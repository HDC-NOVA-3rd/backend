package com.backend.nova.facility.dto;

import com.backend.nova.facility.entity.Facility;
import lombok.Builder;

import java.time.LocalTime;

@Builder
public record FacilityResponse(
        Long facilityId,
        String name,
        String description,
        LocalTime startHour,
        LocalTime endHour,
        boolean reservationAvailable
) {
    // Entity -> DTO 변환 메서드
    public static FacilityResponse from(Facility facility) {
        return FacilityResponse.builder()
                .facilityId(facility.getId())
                .name(facility.getName())
                .description(facility.getDescription())
                .startHour(facility.getStartHour())
                .endHour(facility.getEndHour())
                .reservationAvailable(facility.isReservationAvailable())
                .build();
    }
}
