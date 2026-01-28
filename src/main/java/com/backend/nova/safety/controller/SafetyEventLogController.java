package com.backend.nova.safety.controller;

import com.backend.nova.safety.dto.SafetyEventLogResponse;
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
@RequestMapping({"/api/safety/event/log", "/api/safety/event-log"})
@RequiredArgsConstructor
public class SafetyEventLogController {

    private final SafetyService safetyService;

    @Operation(summary = "안전 이벤트 로그 조회", description = "아파트 기준 이벤트 로그를 최신순으로 조회합니다. (Top-level array)")
    @GetMapping("/apartment/{apartmentId}")
    public ResponseEntity<List<SafetyEventLogResponse>> listSafetyEventLogs(@PathVariable Long apartmentId) {
        return ResponseEntity.ok(safetyService.listSafetyEventLogs(apartmentId));
    }
}
