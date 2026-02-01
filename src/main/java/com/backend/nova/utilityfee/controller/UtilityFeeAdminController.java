package com.backend.nova.utilityfee.controller;

import com.backend.nova.utilityfee.service.UtilityFeeClosingService;
import com.backend.nova.auth.admin.AdminDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;

@RestController
@RequestMapping("/api/admin/utility-fee")
@RequiredArgsConstructor
public class UtilityFeeAdminController {

    private final UtilityFeeClosingService utilityFeeClosingService;

    // =============================
    // 월 마감 처리
    // =============================
    @PostMapping("/close")
    public ResponseEntity<Void> closeMonthlyUtilityFee(
            @RequestParam YearMonth month,
            Authentication authentication
    ) {
        AdminDetails admin = (AdminDetails) authentication.getPrincipal();

        utilityFeeClosingService.closeMonth(
                admin.getApartmentId(),
                month
        );

        return ResponseEntity.ok().build();
    }
}
