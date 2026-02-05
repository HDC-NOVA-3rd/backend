package com.backend.nova.admin.controller;

import com.backend.nova.admin.dto.*;
import com.backend.nova.admin.service.AdminService;
import com.backend.nova.auth.admin.AdminDetails;
import com.backend.nova.member.dto.RefreshTokenRequest;
import com.backend.nova.member.dto.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@Tag(name = "Admin", description = "관리자 회원 관리 API")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    /* ================= 관리자 회원가입 ================= */

    /**
     * 관리자 회원가입 (슈퍼관리자만 가능)
     * POST /api/admin/signup
     */
    @PostMapping("/signup")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> createAdmin(
            @RequestBody @Valid AdminCreateRequest request
    ) {
        adminService.createAdmin(request);
        return ResponseEntity.ok().build();
    }

    /* ================= 로그인 ================= */

    /**
     * 관리자 로그인
     * POST /api/admin/login
     */
    @PostMapping("/login")
    public ResponseEntity<AdminLoginResponse> login(
            @RequestBody AdminLoginRequest request,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(adminService.login(request, httpRequest));
    }

    /**
     * 관리자 로그인 OTP 검증
     * POST /api/admin/login/verify-otp
     */
    @PostMapping("/login/verify-otp")
    public ResponseEntity<AdminLoginResponse> verifyOtp(
            @RequestBody AdminLoginVerifyOtpRequest request,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(adminService.verifyLoginOtp(request, httpRequest));
    }

    /* ================= 로그아웃 ================= */

    /**
     * 관리자 로그아웃
     * POST /api/admin/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@AuthenticationPrincipal AdminDetails adminDetails) {
        adminService.logout(adminDetails);
        return ResponseEntity.ok().build();
    }

    /* ================= 비밀번호 ================= */

    /**
     * 비밀번호 재설정 요청 (G메일 OTP 발송)
     * POST /api/admin/password/reset-request
     */
    @PostMapping("/password/reset-request")
    public ResponseEntity<?> requestPasswordReset(
            @RequestBody AdminPasswordResetRequest request
    ) {
        adminService.requestPasswordReset(request);
        return ResponseEntity.ok().build();
    }

    /**
     * 비밀번호 OTP 검증
     * POST /api/admin/password/verify-otp
     */
    @PostMapping("/password/verify-otp")
    public ResponseEntity<?> passwordVerifyOtp(
            @RequestBody AdminPasswordOtpVerifyRequest request
    ) {
        adminService.passwordVerifyOtp(request);
        return ResponseEntity.ok().build();
    }

    /**
     * 비밀번호 재설정
     * POST /api/admin/password/reset
     */
    @PostMapping("/password/reset")
    public ResponseEntity<?> resetPassword(
            @RequestBody AdminPasswordResetConfirmRequest request
    ) {
        adminService.resetPassword(request);
        return ResponseEntity.ok().build();
    }

    /**
     * 비밀번호 변경 (로그인 상태)
     * PUT /api/admin/password
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/password")
    public ResponseEntity<?> changePassword(
            @RequestBody AdminPasswordChangeRequest request,
            @AuthenticationPrincipal AdminDetails adminDetails
    ) {
        adminService.changePassword(request, adminDetails);
        return ResponseEntity.ok().build();
    }

    /* ================= 토큰 ================= */

    /**
     * Access 토큰 재발급
     * POST /api/admin/refresh
     */
    @Operation(summary = "Access 토큰 재발급", description = "Access 토큰이 만료되는 경우 Refresh 토큰을 사용하여 새로운 Access 토큰을 발급받습니다.")
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @RequestBody RefreshTokenRequest request
    ) {
        return ResponseEntity.ok(adminService.refresh(request));
    }

    /* ================= 내 정보 ================= */

    /**
     * 내 정보 조회
     * GET /api/admin/profile
     */
    @GetMapping("/profile")
    public ResponseEntity<AdminInfoResponse> profile(@AuthenticationPrincipal AdminDetails adminDetails) {
        return ResponseEntity.ok(adminService.getAdminInfo(adminDetails));
    }

    /**
     * 내 아파트 정보 조회
     * GET /api/admin/apartment
     */
    @GetMapping("/apartment")
    public ResponseEntity<AdminApartmentResponse> getMyApartmentInfo(@AuthenticationPrincipal AdminDetails adminDetails) {
        return ResponseEntity.ok(adminService.getAdminApartmentInfo(adminDetails));
    }

    /* ================= 기기 ================= */

    /**
     * 내 기기 목록 조회
     * GET /api/admin/devices
     */
    @GetMapping("/devices")
    public ResponseEntity<List<AdminDeviceResponse>> devices(@AuthenticationPrincipal AdminDetails adminDetails) {
        return ResponseEntity.ok(adminService.getMyDevices(adminDetails));
    }

    /**
     * 기기 삭제(폐기)
     * DELETE /api/admin/devices/{deviceId}
     */
    @DeleteMapping("/devices/{deviceId}")
    public ResponseEntity<?> revoke(
            @AuthenticationPrincipal AdminDetails adminDetails,
            @PathVariable String deviceId,
            HttpServletRequest request
    ) {
        adminService.revokeDevice(adminDetails, deviceId, request.getRemoteAddr());
        return ResponseEntity.ok().build();
    }

    /**
     * 현재 기기 등록 (로그인 과정에서만)
     * POST /api/admin/devices
     */
    @PostMapping("/devices")
    public ResponseEntity<AdminDeviceResponse> registerDevice(@AuthenticationPrincipal AdminDetails adminDetails) {
        return ResponseEntity.ok(adminService.registerCurrentDevice(adminDetails));
    }
}
