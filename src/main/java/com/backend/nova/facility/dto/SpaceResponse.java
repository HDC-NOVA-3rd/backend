package com.backend.nova.facility.dto;

import com.backend.nova.facility.entity.Space;
import lombok.Builder;

@Builder
public record SpaceResponse(
        Long id,
        String name,
        Integer price,
        Integer minCapacity,
        Integer maxCapacity
) {
    // Entity -> DTO 변환 메서드
    public static SpaceResponse from(Space space) {
        return SpaceResponse.builder()
                .id(space.getId())
                .name(space.getName())
                .price(space.getPrice())
                .minCapacity(space.getMinCapacity())
                .maxCapacity(space.getMaxCapacity())
                .build();
    }
}
