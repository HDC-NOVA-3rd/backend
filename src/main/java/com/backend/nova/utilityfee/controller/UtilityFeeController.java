package com.backend.nova.utilityfee.controller;

import com.backend.nova.utilityfee.dto.UtilityFeeRequest;
import com.backend.nova.utilityfee.dto.UtilityFeeResponse;
import com.backend.nova.utilityfee.service.UtilityFeeService;
import com.backend.nova.auth.admin.AdminDetails;
import com.backend.nova.auth.member.MemberDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/utility-fee")
@RequiredArgsConstructor
public class UtilityFeeController {

    private final UtilityFeeService utilityFeeService;

    // =============================
    // 테스트용 공과금 생성 API (운영 제거)
    // =============================
    @PostMapping
    public ResponseEntity<UtilityFeeResponse> createUtilityFee(
            @RequestBody UtilityFeeRequest request
    ) {
        return ResponseEntity.ok(
                utilityFeeService.createUtilityFee(request)
        );
    }

    // =============================
    // 현재 공과금 리스트 조회
    // =============================
    @GetMapping
    public ResponseEntity<List<UtilityFeeResponse>> getCurrentUtilityFees(
            Authentication authentication
    ) {
        Object principal = authentication.getPrincipal();

        if (principal instanceof AdminDetails admin) {
            // 관리자 → 단지 전체 세대 공과금
            return ResponseEntity.ok(
                    utilityFeeService.getCurrentUtilityFeesByApartment(
                            admin.getApartmentId()
                    )
            );
        }

        if (principal instanceof MemberDetails member) {
            // 사용자 → 본인 세대 공과금
            return ResponseEntity.ok(
                    utilityFeeService.getCurrentUtilityFeesByHo(
                            member.getHoId()
                    )
            );
        }

        return ResponseEntity.status(403).build();
    }

    // =============================
    // 개별 공과금 상세 조회
    // =============================
    @GetMapping("/{utilityFeeId}")
    public ResponseEntity<UtilityFeeResponse> getUtilityFee(
            @PathVariable Long utilityFeeId,
            Authentication authentication
    ) {
        Object principal = authentication.getPrincipal();

        if (principal instanceof AdminDetails admin) {
            return ResponseEntity.ok(
                    utilityFeeService.getUtilityFeeForAdmin(
                            utilityFeeId,
                            admin.getApartmentId()
                    )
            );
        }

        if (principal instanceof MemberDetails member) {
            return ResponseEntity.ok(
                    utilityFeeService.getUtilityFeeForMember(
                            utilityFeeId,
                            member.getHoId()
                    )
            );
        }

        return ResponseEntity.status(403).build();
    }
}
