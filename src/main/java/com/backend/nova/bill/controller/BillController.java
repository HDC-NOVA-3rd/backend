package com.backend.nova.bill.controller;

import com.backend.nova.bill.dto.*;
import com.backend.nova.bill.service.BillService;
import com.backend.nova.common.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bill")
@RequiredArgsConstructor
public class BillController {

    private final BillService billService;

    // 본인(입주민) 또는 관리자가 볼 수 있는 전체 고지서 목록
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MEMBER')")
    public ResponseEntity<List<BillResponse>> getMyBills() {
        return ResponseEntity.ok(billService.getMyBills());
    }

    // 고지서 상세 조회 (본인 또는 관리자 소속 범위 내)
    @GetMapping("/{billId}")
    @PreAuthorize("hasAnyRole('ADMIN','MEMBER')")
    public ResponseEntity<BillResponse> getBill(@PathVariable Long billId) {
        return ResponseEntity.ok(billService.getBill(billId));
    }

    // =============================
    // 미납 고지서 목록 (본인 or 아파트 전체)
    // =============================
    @GetMapping("/unpaid")
    @PreAuthorize("hasAnyRole('ADMIN','MEMBER')")
    public ResponseEntity<List<BillResponse>> getUnpaidBills() {
        return ResponseEntity.ok(billService.getUnpaidBills());
    }

    // =============================
    // 특정 월 미납 고지서 목록 (관리자 전용)
    // =============================
    @GetMapping("/unpaid/{month}")   // month 예: "2025-02"
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BillResponse>> getUnpaidBillsByMonth(@PathVariable String month) {
        return ResponseEntity.ok(billService.getUnpaidBillsByMonth(month));
    }

    // =============================
    // 입주민 – 확정 전(OPEN) + READY + PAID 고지서 목록
    // =============================
    @GetMapping("/confirmed-or-ready")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<List<BillResponse>> getConfirmedOrReadyBills() {
        return ResponseEntity.ok(billService.getConfirmedOrReadyBillsForMember());
    }
}