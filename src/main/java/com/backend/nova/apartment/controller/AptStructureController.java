package com.backend.nova.apartment.controller;

import com.backend.nova.apartment.service.AptStructureService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.backend.nova.apartment.dto.ApartmentStructure.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/apartment")
public class AptStructureController {
    private final AptStructureService aptStructureService;

    // 아파트 목록 조회 API
    @GetMapping
    public ResponseEntity<List<ApartmentResponse>> getApartmentList() {
        List<ApartmentResponse> apartments = aptStructureService.getApartmentList();
        return ResponseEntity.ok(apartments);
    }

    // 동 목록 조회 API (아파트 ID 기준)
    @GetMapping("/{apartmentId}/dong")
    public ResponseEntity<List<DongResponse>> getDongList(@PathVariable Long apartmentId) {
        List<DongResponse> dongs = aptStructureService.getDongListByApartmentId(apartmentId);
        return ResponseEntity.ok(dongs);
    }

    // 호 목록 조회 API (동 ID 기준)
    @GetMapping("/dong/{dongId}/ho")
    public ResponseEntity<List<HoResponse>> getHoList(@PathVariable Long dongId) {
        List<HoResponse> hos = aptStructureService.getHoListByDongId(dongId);
        return ResponseEntity.ok(hos);
    }
}
