package com.backend.nova.facility.service;

import com.backend.nova.facility.dto.SpaceResponse;
import com.backend.nova.facility.entity.Space;
import com.backend.nova.facility.repository.SpaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpaceService {
    private final SpaceRepository spaceRepository;

    // 시설 별 공간 목록 조회 (인원 수 필터링 포함)
    public List<SpaceResponse> findAllByFacility(Long facilityId, Integer capacity) {
        List<Space> spaceList;

        if (capacity != null) {
            // 요청 인원을 수용할 수 있는 공간만 조회 (maxCapacity >= capacity >= minCapacity)
            spaceList = spaceRepository.findSpacesByCapacity(facilityId, capacity);
        } else {
            // 전체 조회
            spaceList = spaceRepository.findAllByFacilityId(facilityId);
        }

        return spaceList.stream()
                .map(SpaceResponse::from)
                .collect(Collectors.toList());
    }

    // 공간 상세 조회
    public SpaceResponse findById(Long spaceId) {
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new IllegalArgumentException("해당 공간을 찾을 수 없습니다. id=" + spaceId));
        return SpaceResponse.from(space);
    }
}
