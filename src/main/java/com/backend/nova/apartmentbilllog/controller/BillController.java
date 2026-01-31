package com.backend.nova.apartmentbilllog.controller;

import com.backend.nova.apartmentbilllog.dto.*;
import com.backend.nova.apartmentbilllog.service.BillService;
import com.backend.nova.auth.admin.AdminDetails;
import com.backend.nova.auth.member.MemberDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/bill")
@RequiredArgsConstructor
public class BillController {

    private final BillService billService;

    //결제 테스트 성공을 위한 임시 api
    @PostMapping
    public ResponseEntity<BillResponse> createBill(@RequestBody BillRequest request) {
        return ResponseEntity.ok(billService.createBill(request));
    }

    // 고지서 리스트 조회 (관리자/사용자 분리)
    @GetMapping
    public ResponseEntity<List<BillResponse>> getBills(Authentication authentication) {
        Object principal = authentication.getPrincipal();

        if (principal instanceof AdminDetails admin) {
            // 관리자 → 본인 단지의 모든 세대 고지서 조회
            Long apartmentId = admin.getApartmentId();
            return ResponseEntity.ok(billService.getBillsByApartment(apartmentId));
        } else if (principal instanceof MemberDetails member) {
            // 사용자 → 본인 세대(Ho)의 고지서 리스트 조회
            Long hoId = member.getHoId();
            return ResponseEntity.ok(billService.getBillsByHo(hoId));
        }

        return ResponseEntity.status(403).build();
    }

    // 개별 고지서 상세 조회
    @GetMapping("/{billId}")
    public ResponseEntity<BillResponse> getBill(@PathVariable Long billId,
                                                Authentication authentication) {
        Object principal = authentication.getPrincipal();

        if (principal instanceof AdminDetails admin) {
            return ResponseEntity.ok(billService.getBillForAdmin(billId, admin.getApartmentId()));
        } else if (principal instanceof MemberDetails member) {
            return ResponseEntity.ok(billService.getBillForMember(billId, member.getHoId()));
        }

        return ResponseEntity.status(403).build();
    }

}