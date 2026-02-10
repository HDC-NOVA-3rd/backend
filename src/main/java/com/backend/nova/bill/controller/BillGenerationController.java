package com.backend.nova.bill.controller;

import com.backend.nova.bill.dto.BillGenerateRequest;
import com.backend.nova.bill.service.BillGenerationService;
import com.backend.nova.auth.admin.AdminDetails;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;


@Tag(name = "Bill", description = "고지서 생성 API")
@RestController
@RequestMapping("/api/admin/bill")
@RequiredArgsConstructor
public class BillGenerationController {

    private final BillGenerationService billGenerationService;

    // =============================
    // 월별 고지서 생성
    // =============================
    @Operation(summary = "월별 고지서 생성", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/generate")
    public ResponseEntity<Void> generateBills(
            @Valid @RequestBody BillGenerateRequest request,
            @AuthenticationPrincipal AdminDetails admin
    ) {
        if (admin == null) return ResponseEntity.status(401).build();

        billGenerationService.generateBills(admin.getApartmentId(), request.getMonth());
        return ResponseEntity.ok().build();
    }


}
