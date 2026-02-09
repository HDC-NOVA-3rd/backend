package com.backend.nova.management.controller;

import com.backend.nova.auth.admin.AdminDetails;
import com.backend.nova.management.dto.ManagementFeeRequest;
import com.backend.nova.management.dto.ManagementFeeResponse;
import com.backend.nova.management.service.ManagementFeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/management-fee")
public class ManagementFeeController {

    private final ManagementFeeService managementFeeService;

    /* ===== 내 단지 관리비 항목 조회 ===== */
    @GetMapping
    public ResponseEntity<List<ManagementFeeResponse>> findMyApartmentItems(
            @AuthenticationPrincipal AdminDetails adminDetails
    ) {
        List<ManagementFeeResponse> items =
                managementFeeService.getItemsByApartment(adminDetails.getApartmentId());

        return ResponseEntity.ok(items);
    }

    /* ===== 관리비 항목 등록 ===== */
    @PostMapping
    public ResponseEntity<ManagementFeeResponse> createBillItem(
            @AuthenticationPrincipal AdminDetails adminDetails,
            @RequestBody ManagementFeeRequest request
    ) {
        ManagementFeeResponse created =
                managementFeeService.createItem(adminDetails.getApartmentId(), request);

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /* ===== 관리비 항목 수정 ===== */
    @PutMapping("/{managementFeeId}")
    public ResponseEntity<ManagementFeeResponse> updateBillItem(
            @PathVariable Long managementFeeId,
            @AuthenticationPrincipal AdminDetails adminDetails,
            @RequestBody ManagementFeeRequest request
    ) {
        ManagementFeeResponse updated =
                managementFeeService.updateItem(
                        managementFeeId,
                        adminDetails.getApartmentId(),
                        request
                );

        return ResponseEntity.ok(updated);
    }

    /* ===== 관리비 항목 비활성화 ===== */
    @PatchMapping("/{managementFeeId}/deactivate")
    public ResponseEntity<Void> deactivateBillItem(
            @PathVariable Long managementFeeId,
            @AuthenticationPrincipal AdminDetails adminDetails
    ) {
        managementFeeService.deactivateItem(
                managementFeeId,
                adminDetails.getApartmentId()
        );
        return ResponseEntity.noContent().build();
    }
}

