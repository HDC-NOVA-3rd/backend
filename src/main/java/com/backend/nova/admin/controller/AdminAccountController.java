package com.backend.nova.admin.controller;

import com.backend.nova.admin.dto.*;
import com.backend.nova.admin.service.AdminService;
import com.backend.nova.auth.admin.AdminDetails;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@Tag(name = "Admin-Account", description = "관리자 계정 및 정보 관리 API")
@RestController
@RequestMapping("/api/admin/account")
@RequiredArgsConstructor
public class AdminAccountController {

    private final AdminService adminService;

    /**
     * 관리자 생성 (SUPER_ADMIN 전용)
     */
    @PostMapping("/register")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "관리자 생성", description = "SUPER_ADMIN만 가능", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> createAdmin(
            @RequestBody @Valid AdminCreateRequest request,
            @AuthenticationPrincipal AdminDetails adminDetails
    ) {
        adminService.createAdmin(request, adminDetails.getAdminId());
        return ResponseEntity.ok().build();
    }

    /**
     * 로그아웃
     */
    @Operation(summary = "로그아웃", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletResponse response // 쿠키 삭제를 위해 추가
    ) {
        adminService.logout(response); // 서비스에 response 전달
        return ResponseEntity.ok().build();
    }



    /**
     * 로그인 상태 비밀번호 변경 요청 (현재 비밀번호 검증 + OTP 발송)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/password/change/request")
    @Operation(summary = "비밀번호 변경 요청", description = "로그인 상태에서 OTP 발송", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<AdminMessageResponse> requestChangePassword(
            @RequestBody @Valid AdminPasswordChangeRequest request,
            @AuthenticationPrincipal AdminDetails adminDetails
    ) {
        return ResponseEntity.ok(adminService.requestChangePassword(request, adminDetails.getAdminId()));
    }

    /**
     * 로그인 상태 비밀번호 변경 확정 (OTP 검증 + 새 비밀번호 설정)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/password/change/confirm")
    @Operation(summary = "비밀번호 변경 확정", description = "OTP 검증 후 새 비밀번호 적용", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<AdminMessageResponse> confirmChangePassword(
            @RequestBody @Valid AdminPasswordChangeConfirmRequest request,
            @AuthenticationPrincipal AdminDetails adminDetails
    ) {
        return ResponseEntity.ok(adminService.confirmChangePassword(request, adminDetails.getAdminId()));
    }

    /**
     * 내 정보 조회
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/profile")
    @Operation(summary = "내 정보 조회", description = "로그인한 관리자 정보 조회", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<AdminInfoResponse> getMyInfo(
            @AuthenticationPrincipal AdminDetails adminDetails
    ) {
        return ResponseEntity.ok(adminService.getAdminInfo(adminDetails.getAdminId()));
    }

    /**
     * 내 아파트 정보 조회
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/apartment")
    @Operation(summary = "내 아파트 정보 조회", description = "로그인한 관리자의 단지 정보 조회", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<AdminApartmentResponse> getMyApartmentInfo(
            @AuthenticationPrincipal AdminDetails adminDetails
    ) {
        return ResponseEntity.ok(adminService.getAdminApartmentInfo(adminDetails.getAdminId()));
    }
}
