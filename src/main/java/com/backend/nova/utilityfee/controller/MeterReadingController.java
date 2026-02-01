package com.backend.nova.utilityfee.controller;

import com.backend.nova.utilityfee.dto.MeterReadingResponse;
import com.backend.nova.utilityfee.service.MeterReadingService;
import com.backend.nova.auth.admin.AdminDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/meter-reading")
@RequiredArgsConstructor
public class MeterReadingController {

    private final MeterReadingService meterReadingService;

    // =============================
    // 단지 전체 미터기 조회 (관리자)
    // =============================
    @GetMapping
    public ResponseEntity<List<MeterReadingResponse>> getMeterReadings(
            Authentication authentication
    ) {
        AdminDetails admin = (AdminDetails) authentication.getPrincipal();

        return ResponseEntity.ok(
                meterReadingService.getMeterReadingsByApartment(
                        admin.getApartmentId()
                )
        );
    }

    // =============================
    // 개별 미터기 상세 조회 (관리자)
    // =============================
    @GetMapping("/{meterReadingId}")
    public ResponseEntity<MeterReadingResponse> getMeterReading(
            @PathVariable Long meterReadingId,
            Authentication authentication
    ) {
        AdminDetails admin = (AdminDetails) authentication.getPrincipal();

        return ResponseEntity.ok(
                meterReadingService.getMeterReading(
                        meterReadingId,
                        admin.getApartmentId()
                )
        );
    }
}
