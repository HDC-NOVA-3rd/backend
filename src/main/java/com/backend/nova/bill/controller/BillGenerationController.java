package com.backend.nova.bill.controller;

import com.backend.nova.bill.dto.*;
import com.backend.nova.bill.service.BillGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/admin/bill")
@RequiredArgsConstructor
public class BillGenerationController {

    private final BillGenerationService billGenerationService;

    //결제 테스트 성공을 위한 임시 api
    @PostMapping
    public ResponseEntity<BillResponse> createBill(@RequestBody BillRequest request) {
        return ResponseEntity.ok(billGenerationService.createBill(request));
    }

    // =============================
    // 고지서 발행 (관리자)
    // =============================
    @PostMapping("/issue")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BillResponse> issueBill(
            @RequestBody BillReadyRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                billGenerationService.issueBill(request, authentication)
        );
    }

    // =============================
// 월별 고지서 일괄 발행
// =============================
    @PostMapping("/issue/bulk")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BillBulkReadyResponse> issueBillsBulk(
            @RequestBody BillBulkReadyRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                billGenerationService.issueBillsForApartment(request, authentication)
        );
    }


    // =============================
    // 월별 미터기 고지서 생성
    // =============================
//    @PostMapping("/generate")
//    public ResponseEntity<Void> generateBills(
//            @RequestParam YearMonth month,
//            Authentication authentication
//    ) {
//        AdminDetails admin = (AdminDetails) authentication.getPrincipal();
//
//        billGenerationService.generateMeterBills(
//                admin.getApartmentId(),
//                month
//        );
//
//        return ResponseEntity.ok().build();
//    }
}
