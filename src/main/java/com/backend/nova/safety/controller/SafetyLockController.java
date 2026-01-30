package com.backend.nova.safety.controller;

import com.backend.nova.safety.dto.SafetyLockRequest;
import com.backend.nova.safety.dto.SafetyLockResponse;
import com.backend.nova.safety.service.SafetyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Safety", description = "Safety API")
@RestController
@RequestMapping("/api/safety/facility")
@RequiredArgsConstructor
public class SafetyLockController {

    private final SafetyService safetyService;

    @Operation(summary = "시설 예약 차단/해제", description = "facilityId 기준 reservationAvailable(true/false)을 설정합니다.")
    @PostMapping("/lock")
    public ResponseEntity<SafetyLockResponse> updateFacilityReservationLock(@RequestBody(required = false) SafetyLockRequest request) {
        if (request == null || request.facilityId() == null || request.facilityId() <= 0 || request.reservationAvailable() == null) {
            return ResponseEntity.badRequest().build();
        }
        SafetyLockResponse response = safetyService.updateFacilityReservationLock(request);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }
}
