package com.backend.nova.safety.controller;

import com.backend.nova.resident.dto.ResidentRequestDto;
import com.backend.nova.resident.dto.ResidentResponseDto;
import com.backend.nova.resident.service.ResidentService;
import com.backend.nova.safety.dto.SafetyEventLogResponse;
import com.backend.nova.safety.dto.SafetyLockRequest;
import com.backend.nova.safety.dto.SafetySensorLogResponse;
import com.backend.nova.safety.dto.SafetyStatusResponse;
import com.backend.nova.safety.service.SafetyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@Tag(name = "Safety", description = "화재감지 API")
@RestController
@RequestMapping("/api/safety")
@RequiredArgsConstructor
public class SafetyController {
    private final SafetyService safetyService;

    @Operation(summary = "현재 안전 상태 리스트 조회", description = "아파트 id로 단지 내 구역 현재 안전 상태 리스트 조회합니다")
    @GetMapping("/status/apartment/{apartmentId}")
    public ResponseEntity<List<SafetyStatusResponse>> getResident(@PathVariable Long apartmentId) {
        List<SafetyStatusResponse> statusList = safetyService.getStatusList(apartmentId);
        return ResponseEntity.ok(statusList);
    }

    @Operation(summary = "센서 로그 조회", description = "센서 로그를 조회합니다.")
    @GetMapping("/sensor/log/apartment/{apartmentId}")
    public ResponseEntity<List<SafetySensorLogResponse>> getSensorLogList(@PathVariable Long apartmentId) {
        List<SafetySensorLogResponse> sensorLogList = safetyService.getSensorLogList(apartmentId);
        return ResponseEntity.ok(sensorLogList);
    }

    @Operation(summary = "화재/안전 이벤트 단지 전체 로그 조회", description = "화재/안전 이벤트 단지 전체 로그 조회합니다.")
    @GetMapping("/event/log/apartment/{apartmentId}")
    public ResponseEntity<List<SafetyEventLogResponse>> getEventLogList(@PathVariable Long apartmentId) {
        List<SafetyEventLogResponse> eventLogResponseList  = safetyService.getEventLogList(apartmentId);
        return ResponseEntity.ok(eventLogResponseList);
    }

    @Operation(summary = "시설 예약 차단/해제", description = "시설 예약 차단/해제합니다.")
    @PutMapping("/facility/lock")
    public ResponseEntity<Void> updateReservationAvailable(@PathVariable Long facilityId, @RequestBody SafetyLockRequest requestDto) {
        safetyService.updateReservationAvailable(facilityId, requestDto);
        return ResponseEntity.ok().build();
    }
}