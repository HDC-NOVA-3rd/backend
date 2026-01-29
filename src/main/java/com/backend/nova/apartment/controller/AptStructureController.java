package com.backend.nova.apartment.controller;

import com.backend.nova.apartment.service.AptStructureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.backend.nova.apartment.dto.ApartmentStructure.*;

import java.util.List;

@Tag(name = "아파트 동 호수 정보 조회 ", description = "아파트 단지, 동, 호로 이어지는 정보를 조회하는 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/apartment")
public class AptStructureController {
    private final AptStructureService aptStructureService;

    // 아파트 목록 조회 API
    @Operation(
            summary = "전체 아파트 목록 조회",
            description = "서비스에 등록된 모든 아파트 단지의 기본 정보(ID, 이름 등)를 리스트 형태로 반환합니다."
    )
    @GetMapping
    public ResponseEntity<List<ApartmentResponse>> getApartmentList() {
        List<ApartmentResponse> apartments = aptStructureService.getApartmentList();
        return ResponseEntity.ok(apartments);
    }

    // 동 목록 조회 API (아파트 ID 기준)
    @Operation(
            summary = "특정 아파트의 동(Dong) 목록 조회",
            description = "선택한 아파트 ID에 소속된 모든 동(예: 101동, 102동) 리스트를 반환합니다."
    )
    @GetMapping("/{apartmentId}/dong")
    public ResponseEntity<List<DongResponse>> getDongList(@PathVariable Long apartmentId) {
        List<DongResponse> dongs = aptStructureService.getDongListByApartmentId(apartmentId);
        return ResponseEntity.ok(dongs);
    }

    // 호 목록 조회 API (동 ID 기준)
    @Operation(
            summary = "특정 동의 호(Ho) 목록 조회",
            description = "선택한 동 ID에 소속된 모든 호(예: 101호, 102호) 리스트를 반환합니다."
    )
    @GetMapping("/dong/{dongId}/ho")
    public ResponseEntity<List<HoResponse>> getHoList(@PathVariable Long dongId) {
        List<HoResponse> hos = aptStructureService.getHoListByDongId(dongId);
        return ResponseEntity.ok(hos);
    }
}
