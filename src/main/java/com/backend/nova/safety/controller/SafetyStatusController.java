package com.backend.nova.safety.controller;

import com.backend.nova.safety.dto.SafetyStatusResponse;
import com.backend.nova.safety.service.SafetyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Safety", description = "Safety API")
@RestController
@RequestMapping("/api/safety/status")
@RequiredArgsConstructor
public class SafetyStatusController {

    private final SafetyService safetyService;

    @Operation(summary = "안전 상태 리스트 조회", description = "아파트 기준 안전 상태 리스트를 조회합니다. (Top-level array)")
    @GetMapping("/apartment/{apartmentId}")
    public ResponseEntity<List<SafetyStatusResponse>> listSafetyStatus(@PathVariable Long apartmentId) {
        return ResponseEntity.ok(safetyService.listSafetyStatus(apartmentId));
    }
}
