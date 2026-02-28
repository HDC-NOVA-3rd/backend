package com.backend.nova.facility.dto;

import com.backend.nova.facility.entity.FacilityImage;
import com.backend.nova.facility.entity.Space;
import com.backend.nova.facility.entity.SpaceImage;
import lombok.Builder;

import java.util.Comparator;
import java.util.List;

@Builder
public record SpaceResponse(
        Long id,
        String name,
        Integer price,
        Integer minCapacity,
        Integer maxCapacity,
        List<String> imageUrls
) {
    // Entity -> DTO 변환 메서드
    public static SpaceResponse from(Space space) {
        List<String> urls = space.getImages().stream()
                .sorted(Comparator.comparing(SpaceImage::getDisplayOrder)) // 순서대로 정렬
                .map(SpaceImage::getImageUrl)
                .toList();

        return SpaceResponse.builder()
                .id(space.getId())
                .name(space.getName())
                .price(space.getPrice())
                .minCapacity(space.getMinCapacity())
                .maxCapacity(space.getMaxCapacity())
                .imageUrls(urls)
                .build();
    }
}
