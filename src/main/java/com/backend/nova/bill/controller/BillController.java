package com.backend.nova.bill.controller;

import com.backend.nova.bill.dto.*;
import com.backend.nova.bill.service.BillService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/bill")
@RequiredArgsConstructor
public class BillController {

    private final BillService billService;

    // 고지서 리스트 조회
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MEMBER')")
    public ResponseEntity<List<BillResponse>> getBills(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                billService.getBills(authentication)
        );
    }

    // 고지서 상세 조회
    @GetMapping("/{billId}")
    @PreAuthorize("hasAnyRole('ADMIN','MEMBER')")
    public ResponseEntity<BillResponse> getBill(
            @PathVariable Long billId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                billService.getBill(billId, authentication)
        );
    }


    // =============================
// 미납 고지서 조회
// =============================
    @GetMapping("/unpaid")
    @PreAuthorize("hasAnyRole('ADMIN','MEMBER')")
    public ResponseEntity<List<BillResponse>> getUnpaidBills(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                billService.getUnpaidBills(authentication)
        );
    }

    @GetMapping("/unpaid/{month}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BillResponse>> getUnpaidBillsByMonth(
            @PathVariable String month,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                billService.getUnpaidBillsByMonth(month, authentication)
        );
    }



}
