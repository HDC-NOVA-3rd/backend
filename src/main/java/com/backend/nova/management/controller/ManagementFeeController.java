package com.backend.nova.management.controller;

import com.backend.nova.auth.admin.AdminDetails;
import com.backend.nova.management.dto.ManagementFeeCreateRequest;
import com.backend.nova.management.dto.ManagementFeeResponse;
import com.backend.nova.management.dto.ManagementFeeUpdateRequest;
import com.backend.nova.management.service.ManagementFeeService;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/management-fee")
public class ManagementFeeController {

    private final ManagementFeeService managementFeeService;

    /* ===== 조회 ===== */
    @GetMapping
    public ResponseEntity<List<ManagementFeeResponse>> findItems(
            @AuthenticationPrincipal @Parameter(hidden = true) AdminDetails adminDetails
    ) {
        // SecurityContext 확인 로그
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("[Controller] SecurityContext auth: {}", auth);

        // AdminDetails 확인
        if (adminDetails == null) {
            // SecurityContext에서 principal 확인
            Object principal = (auth != null) ? auth.getPrincipal() : null;
            log.warn("[Controller] @AuthenticationPrincipal is null, principal from SecurityContext: {}", principal);

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("[Controller] AdminDetails: {}", adminDetails);

        // 실제 서비스 호출
        List<ManagementFeeResponse> items = managementFeeService.getItems(adminDetails.getApartmentId(), null);
        log.info("[Controller] Retrieved {} management fees", items.size());

        return ResponseEntity.ok(items);
    }

    /* ===== 등록 ===== */
    @PostMapping
    public ResponseEntity<ManagementFeeResponse> create(
            @AuthenticationPrincipal @Parameter(hidden = true) AdminDetails adminDetails,
            @RequestBody ManagementFeeCreateRequest request
    ) {
        log.info("[Controller] Creating management fee by admin: {}", adminDetails);

        ManagementFeeResponse response = managementFeeService.createItem(adminDetails.getApartmentId(), request);
        log.info("[Controller] Created management fee with id: {}", response.id());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /* ===== 수정 ===== */
    @PutMapping("/{feeId}")
    public ResponseEntity<ManagementFeeResponse> update(
            @PathVariable Long feeId,
            @AuthenticationPrincipal @Parameter(hidden = true) AdminDetails adminDetails,
            @RequestBody ManagementFeeUpdateRequest request
    ) {
        log.info("[Controller] Updating management fee {} by admin {}", feeId, adminDetails);

        ManagementFeeResponse response = managementFeeService.updateItem(feeId, adminDetails.getApartmentId(), request);
        log.info("[Controller] Updated management fee: {}", response.id());

        return ResponseEntity.ok(response);
    }

    /* ===== 삭제 ===== */
    @PatchMapping("/{feeId}/deactivate")
    public ResponseEntity<Void> deactivate(
            @PathVariable Long feeId,
            @AuthenticationPrincipal @Parameter(hidden = true) AdminDetails adminDetails
    ) {
        log.info("[Controller] Deactivating management fee {} by admin {}", feeId, adminDetails);

        managementFeeService.deactivateItem(feeId, adminDetails.getApartmentId());
        log.info("[Controller] Deactivated management fee {}", feeId);

        return ResponseEntity.noContent().build();
    }

    /* ===== 복구 ===== */
    @PatchMapping("/{feeId}/restore")
    public ResponseEntity<Void> restore(
            @PathVariable Long feeId,
            @AuthenticationPrincipal @Parameter(hidden = true) AdminDetails adminDetails
    ) {
        log.info("[Controller] Restoring management fee {} by admin {}", feeId, adminDetails);

        managementFeeService.restoreItem(feeId, adminDetails.getApartmentId());
        log.info("[Controller] Restored management fee {}", feeId);

        return ResponseEntity.noContent().build();
    }
}
