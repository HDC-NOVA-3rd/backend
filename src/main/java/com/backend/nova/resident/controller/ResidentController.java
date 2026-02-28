package com.backend.nova.resident.controller;

import com.backend.nova.auth.admin.AdminDetails;
import com.backend.nova.resident.dto.ResidentSaveRequest;
import com.backend.nova.resident.dto.ResidentRequest;
import com.backend.nova.resident.dto.ResidentResponse;
import com.backend.nova.resident.dto.ResidentVerifyResponse;
import com.backend.nova.resident.service.ResidentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@Tag(name = "Resident", description = "입주민 관리 API (관리자 전용)")
@RestController
@RequestMapping("/api/resident")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ResidentController {

    private final ResidentService residentService;

    @Operation(summary = "입주민 상세 조회", description = "입주민 ID로 상세 정보를 조회합니다.")
    @GetMapping("/{residentId}")
    public ResponseEntity<ResidentResponse> getResident(
            @PathVariable Long residentId,
            @AuthenticationPrincipal AdminDetails adminDetails) {

        return ResponseEntity.ok(
                residentService.getResident(residentId, adminDetails.getApartmentId())
        );
    }

    @Operation(summary = "아파트별 입주민 목록 조회", description = "페이징과 검색 조건을 포함하여 조회합니다.")
    @GetMapping("/apartment")
    public ResponseEntity<Page<ResidentResponse>> getAllResidents(
            @RequestParam(required = false) Long dongId,
            @RequestParam(required = false) String searchTerm,
            @Parameter(description = "페이지 번호(0부터), 페이지 크기 등") Pageable pageable,
            @AuthenticationPrincipal AdminDetails adminDetails) {

        Long apartmentId = adminDetails.getApartmentId();

        return ResponseEntity.ok(
                residentService.getAllResidents(apartmentId, dongId, searchTerm, pageable)
        );
    }

    @Operation(summary = "입주민 등록", description = "새로운 입주민을 등록합니다.")
    @PostMapping
    public ResponseEntity<?> createResident(
            @RequestBody ResidentSaveRequest request,
            @AuthenticationPrincipal AdminDetails adminDetails) {

        Long residentId = residentService.createResident(request, adminDetails.getApartmentId());
        return ResponseEntity.created(URI.create("/api/resident/" + residentId)).build();
    }

    @Operation(summary = "입주민 정보 수정", description = "입주민 정보를 수정합니다.")
    @PutMapping("/{residentId}")
    public ResponseEntity<Void> updateResident(
            @PathVariable Long residentId,
            @RequestBody ResidentSaveRequest request,
            @AuthenticationPrincipal AdminDetails adminDetails) {

        residentService.updateResident(
                residentId,
                request,
                adminDetails.getApartmentId()
        );
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "입주민 삭제", description = "입주민 ID로 입주민을 삭제합니다.")
    @DeleteMapping("/{residentId}")
    public ResponseEntity<Void> deleteResident(
            @PathVariable Long residentId,
            @AuthenticationPrincipal AdminDetails adminDetails) {

        residentService.deleteResident(
                residentId,
                adminDetails.getApartmentId()
        );
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "세대 입주민 리스트 삭제", description = "호 ID로 해당 세대의 입주민을 모두 삭제합니다.")
    @DeleteMapping("/ho/{hoId}")
    public ResponseEntity<Void> deleteAllResidents(
            @PathVariable Long hoId,
            @AuthenticationPrincipal AdminDetails adminDetails) {

        residentService.deleteAllResidents(
                hoId,
                adminDetails.getApartmentId()
        );
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "입주민 정보 검증", description = "입주민 정보(호 ID, 이름, 전화번호)가 일치하는지 확인합니다.", security = {})
    @PostMapping("/verify")
    public ResponseEntity<ResidentVerifyResponse> verifyResident(@RequestBody ResidentRequest request) {
        ResidentVerifyResponse verifyResDto = residentService.verifyResident(request);
        return ResponseEntity.ok(verifyResDto);
    }
}