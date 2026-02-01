package com.backend.nova.bill.controller;

import com.backend.nova.bill.service.BillGenerationService;
import com.backend.nova.auth.admin.AdminDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;

@RestController
@RequestMapping("/api/admin/bill")
@RequiredArgsConstructor
public class BillAdminController {

    private final BillGenerationService billGenerationService;

    // =============================
    // 월별 고지서 생성
    // =============================
    @PostMapping("/generate")
    public ResponseEntity<Void> generateBills(
            @RequestParam YearMonth month,
            Authentication authentication
    ) {
        AdminDetails admin = (AdminDetails) authentication.getPrincipal();

        billGenerationService.generateBills(
                admin.getApartmentId(),
                month
        );

        return ResponseEntity.ok().build();
    }
}
