package com.backend.nova.apartmentcurrentbill.controller;

import com.backend.nova.apartmentcurrentbill.dto.*;
import com.backend.nova.apartmentcurrentbill.service.CurrentBillService;
import com.backend.nova.auth.admin.AdminDetails;
import com.backend.nova.auth.member.MemberDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/current-bill")
@RequiredArgsConstructor
public class CurrentBillController {

    private final CurrentBillService currentBillService;

    //결제 테스트 성공을 위한 임시 api
    @PostMapping
    public ResponseEntity<CurrentBillResponse> createCurrentBill(@RequestBody CurrentBillRequest request) {
        return ResponseEntity.ok(currentBillService.createCurrentBill(request));
    }

    // 현재 고지서 리스트 조회
    @GetMapping
    public ResponseEntity<List<CurrentBillResponse>> getCurrentBills(Authentication authentication) {
        Object principal = authentication.getPrincipal();

        if (principal instanceof AdminDetails admin) {
            // 관리자 → 본인 단지의 모든 세대 현재 고지서 조회
            Long apartmentId = admin.getApartmentId();
            return ResponseEntity.ok(currentBillService.getCurrentBillsByApartment(apartmentId));
        } else if (principal instanceof MemberDetails member) {
            // 사용자 → 본인 세대(Ho)의 현재 고지서 조회
            Long hoId = member.getHoId();
            return ResponseEntity.ok(currentBillService.getCurrentBillsByHo(hoId));
        }

        return ResponseEntity.status(403).build();
    }

    // 개별 현재 고지서 상세 조회
    @GetMapping("/{currentBillId}")
    public ResponseEntity<CurrentBillResponse> getCurrentBill(@PathVariable Long currentBillId,
                                                              Authentication authentication) {
        Object principal = authentication.getPrincipal();

        if (principal instanceof AdminDetails admin) {
            return ResponseEntity.ok(currentBillService.getCurrentBillForAdmin(currentBillId, admin.getApartmentId()));
        } else if (principal instanceof MemberDetails member) {
            return ResponseEntity.ok(currentBillService.getCurrentBillForMember(currentBillId, member.getHoId()));
        }

        return ResponseEntity.status(403).build();
    }

}