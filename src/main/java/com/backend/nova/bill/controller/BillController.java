package com.backend.nova.bill.controller;

import com.backend.nova.bill.dto.*;
import com.backend.nova.bill.service.BillService;
import com.backend.nova.auth.admin.AdminDetails;
import com.backend.nova.auth.member.MemberDetails;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.util.List;

@Tag(name = "Bill", description = "고지서 조회 API")
@RestController
@RequestMapping("api/bill")
@RequiredArgsConstructor
public class BillController {

    private final BillService billService;

    // 고지서 리스트 조회 (관리자/사용자 분리)
    @Operation(summary = "고지서 조회", security = @SecurityRequirement(name = "bearerAuth"))
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
    @Operation(summary = "개별 고지서 상세 조회", security = @SecurityRequirement(name = "bearerAuth"))
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