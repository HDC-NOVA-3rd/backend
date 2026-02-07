package com.backend.nova.facility.controller;

import com.backend.nova.facility.dto.FacilityResponse;
import com.backend.nova.facility.dto.SpaceResponse;
import com.backend.nova.facility.service.FacilityService;
import com.backend.nova.facility.service.SpaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "커뮤니티 시설/공간 관리", description = "시설(Facility) 상세 정보 및 내부 공간(Space) 조회 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/facility")
@SecurityRequirement(name = "bearerAuth")
public class FacilityController {
    private final FacilityService facilityService;
    private final SpaceService spaceService;

    // 특정 시설의 기본 정보 조회
    @Operation(
            summary = "시설 상세 정보 조회",
            description = "시설 ID(PK)를 기반으로 특정 시설(예: 헬스장, 독서실, 골프연습장)의 운영 시간, 예약 가능 여부 등 기본 정보를 조회합니다."
    )
    @GetMapping("/{facilityId}")
    public ResponseEntity<FacilityResponse> getFacility(@PathVariable Long facilityId){
        FacilityResponse facility = facilityService.getFacility(facilityId);
        return ResponseEntity.ok(facility);
    }
    // 특정 시설의 공간 목록 조회 (필터링 포함 - 가용 인원 수)
    @Operation(
            summary = "시설 내 공간 목록 조회 (인원 수 필터링 포함)",
            description = "특정 시설에 속한 공간(Room, 좌석 등) 목록을 조회합니다. " +
                    "쿼리 파라미터로 **인원 수(capacity)**를 입력하면, 해당 인원을 수용할 수 있는(최소~최대 범위 내) 공간만 필터링하여 반환합니다."
    )
    @GetMapping("/{facilityId}/space")
    public ResponseEntity<List<SpaceResponse>> getSpaceList(
            @PathVariable Long facilityId,
            @RequestParam(required = false) Integer capacity) {
        return ResponseEntity.ok(spaceService.findAllByFacility(facilityId, capacity));
    }

    // 공간 상세 조회 (space ID 기반)
    @Operation(
            summary = "공간 상세 정보 조회",
            description = "공간 ID(PK)를 이용하여 특정 공간(예: 미팅룸 A, 101번 좌석)의 상세 정보(가격, 수용 인원 등)를 조회합니다."
    )
    @GetMapping("/space/{spaceId}")
    public ResponseEntity<SpaceResponse> getSpace(@PathVariable Long spaceId) {
        return ResponseEntity.ok(spaceService.findById(spaceId));
    }
}
