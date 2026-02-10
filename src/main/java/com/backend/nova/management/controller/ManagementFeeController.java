package com.backend.nova.management.controller;

import com.backend.nova.auth.admin.AdminDetails;
import com.backend.nova.management.dto.ManagementFeeCreateRequest;
import com.backend.nova.management.dto.ManagementFeeResponse;
import com.backend.nova.management.dto.ManagementFeeUpdateRequest;
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

    /* ===== 조회 ===== */
    @GetMapping
    public ResponseEntity<List<ManagementFeeResponse>> findItems(
            @AuthenticationPrincipal AdminDetails adminDetails,
            @RequestParam(required = false) Boolean active
    ) {
        return ResponseEntity.ok(
                managementFeeService.getItems(
                        adminDetails.getApartmentId(),
                        active
                )
        );
    }


    /* ===== 등록 ===== */
    @PostMapping
    public ResponseEntity<ManagementFeeResponse> create(
            @AuthenticationPrincipal AdminDetails adminDetails,
            @RequestBody ManagementFeeCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(managementFeeService.createItem(
                        adminDetails.getApartmentId(), request));
    }

    /* ===== 수정 ===== */
    @PutMapping("/{id}")
    public ResponseEntity<ManagementFeeResponse> update(
            @PathVariable Long id,
            @AuthenticationPrincipal AdminDetails adminDetails,
            @RequestBody ManagementFeeUpdateRequest request
    ) {
        return ResponseEntity.ok(
                managementFeeService.updateItem(
                        id,
                        adminDetails.getApartmentId(),
                        request
                )
        );
    }

    /* ===== 삭제 ===== */
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivate(
            @PathVariable Long id,
            @AuthenticationPrincipal AdminDetails adminDetails
    ) {
        managementFeeService.deactivateItem(id, adminDetails.getApartmentId());
        return ResponseEntity.noContent().build();
    }

    /* ===== 복구 ===== */
    @PatchMapping("/{id}/restore")
    public ResponseEntity<Void> restore(
            @PathVariable Long id,
            @AuthenticationPrincipal AdminDetails adminDetails
    ) {
        managementFeeService.restoreItem(id, adminDetails.getApartmentId());
        return ResponseEntity.noContent().build();
    }
}


