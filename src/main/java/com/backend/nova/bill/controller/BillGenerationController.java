package com.backend.nova.bill.controller;

import com.backend.nova.bill.dto.BillRequest;
import com.backend.nova.bill.dto.BillResponse;
import com.backend.nova.bill.service.BillGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
