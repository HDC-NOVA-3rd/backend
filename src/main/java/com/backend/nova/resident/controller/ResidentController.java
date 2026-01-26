package com.backend.nova.resident.controller;

import com.backend.nova.resident.dto.ResidentRequestDto;
import com.backend.nova.resident.dto.ResidentResponseDto;
import com.backend.nova.resident.dto.ResidentVerifyResponseDto;
import com.backend.nova.resident.service.ResidentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@Tag(name = "Resident", description = "입주민 관리 API")
@RestController
@RequestMapping("/api/resident")
@RequiredArgsConstructor
public class ResidentController {

    private final ResidentService residentService;

    @Operation(summary = "입주민 상세 조회", description = "입주민 ID로 상세 정보를 조회합니다.")
    @GetMapping("/{residentId}")
    public ResponseEntity<ResidentResponseDto> getResident(@PathVariable Long residentId) {
        ResidentResponseDto resident = residentService.getResident(residentId);
        return ResponseEntity.ok(resident);
    }

    @Operation(summary = "아파트별 입주민 목록 조회", description = "아파트 ID로 해당 아파트의 모든 입주민을 조회합니다.")
    @GetMapping("/apartment/{apartmentId}")
    public ResponseEntity<List<ResidentResponseDto>> getAllResidents(@PathVariable Long apartmentId) {
        List<ResidentResponseDto> residents = residentService.getAllResidents(apartmentId);
        return ResponseEntity.ok(residents);
    }

    @Operation(summary = "입주민 등록", description = "새로운 입주민을 등록합니다.")
    @PostMapping
    public ResponseEntity<?> createResident(@RequestBody ResidentRequestDto requestDto) {
        try {
            Long residentId = residentService.createResident(requestDto);
            return ResponseEntity.created(URI.create("/api/resident/" + residentId)).build();
        } catch (DataIntegrityViolationException e) {
            // DB unique 제약조건 위반 시 발생
            return ResponseEntity.badRequest().body("이미 등록된 휴대폰 번호입니다.");
        }
    }

    @Operation(summary = "입주민 정보 수정", description = "입주민 정보를 수정합니다.")
    @PutMapping("/{residentId}")
    public ResponseEntity<Void> updateResident(@PathVariable Long residentId, @RequestBody ResidentRequestDto requestDto) {
        residentService.updateResident(residentId, requestDto);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "입주민 삭제", description = "입주민 ID로 입주민을 삭제합니다.")
    @DeleteMapping("/{residentId}")
    public ResponseEntity<Void> deleteResident(@PathVariable Long residentId) {
        residentService.deleteResident(residentId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "세대 입주민 리스트 삭제", description = "호 ID로 해당 세대의 입주민을 모두 삭제합니다.")
    @DeleteMapping("/ho/{hoId}")
    public ResponseEntity<Void> deleteAllResidents(@PathVariable Long hoId) {
        residentService.deleteAllResidents(hoId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "입주민 정보 검증", description = "입주민 정보(호 ID, 이름, 전화번호)가 일치하는지 확인합니다.")
    @PostMapping("/verify")
    public ResponseEntity<ResidentVerifyResponseDto> verifyResident(@RequestBody ResidentRequestDto requestDto) {
        ResidentVerifyResponseDto verifyResDto = residentService.verifyResident(requestDto);
        return ResponseEntity.ok(verifyResDto);
    }
}