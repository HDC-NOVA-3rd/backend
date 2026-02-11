package com.backend.nova.facility.dto;

import com.backend.nova.facility.entity.Facility;
import com.backend.nova.facility.entity.FacilityImage;
import com.backend.nova.facility.entity.Space;
import lombok.Builder;

import java.time.LocalTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

@Builder
public record FacilityResponse(
        Long facilityId,
        String name,
        String description,
        LocalTime startHour,
        LocalTime endHour,
        boolean reservationAvailable,
        Integer minCapacity,
        Integer maxCapacity,
        List<String> imageUrls
) {
    // Entity -> DTO 변환 메서드
    public static FacilityResponse from(Facility facility) {
        Collection<Space> spaces = facility.getSpaces();

        int minCap = 0;
        int maxCap = 0;

        // 공간이 하나라도 있으면 계산
        if (spaces != null && !spaces.isEmpty()) {
            minCap = spaces.stream()
                    .mapToInt(Space::getMinCapacity)
                    .min().orElse(0);

            maxCap = spaces.stream()
                    .mapToInt(Space::getMaxCapacity)
                    .max().orElse(0);
        }

        List<String> urls = facility.getImages().stream()
                .sorted(Comparator.comparing(FacilityImage::getDisplayOrder)) // 순서대로 정렬
                .map(FacilityImage::getImageUrl)
                .toList();

        return FacilityResponse.builder()
                .facilityId(facility.getId())
                .name(facility.getName())
                .description(facility.getDescription())
                .startHour(facility.getStartHour())
                .endHour(facility.getEndHour())
                .reservationAvailable(facility.isReservationAvailable())
                .minCapacity(minCap)
                .maxCapacity(maxCap)
                .imageUrls(urls)
                .build();
    }
}
