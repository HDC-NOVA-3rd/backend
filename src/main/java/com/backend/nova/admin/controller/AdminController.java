package com.backend.nova.admin.controller;

import com.backend.nova.admin.dto.*;
import com.backend.nova.admin.entity.Admin;
import com.backend.nova.admin.service.AdminService;
import com.backend.nova.admin.dto.AdminApartmentResponse;
import com.backend.nova.admin.dto.AdminInfoResponse;
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

@Tag(name = "Admin", description = "관리자 회원 관리 API")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    /**
     * 관리자 회원가입 (슈퍼관리자만 가능)
     * POST /api/admin
     */
    @PostMapping("/signup")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> createAdmin(
            @RequestBody @Valid AdminCreateRequest request
    ) {
        adminService.createAdmin(request);
        return ResponseEntity.ok().build();
    }

    /**
     * 관리자 로그인
     * POST /api/admin/login
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid AdminLoginRequest request) {
        TokenResponse response = adminService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 슈퍼관리자 로그인시 otp인증
     * POST /api/admin/login/verify-otp
     */
    // @PostMapping("/login/verify-otp")
    // @PreAuthorize("hasRole('SUPER_ADMIN')")
    // public ResponseEntity<?> loginVerifyOtp(@RequestBody AdminLoginOtpVerifyRequest request) {
    //     TokenResponse response = adminAuthService.loginVerifyOtp(request);
    //     return ResponseEntity.ok(response);
    // }

    /**
     * 관리자 로그아웃
     * POST /api/admin/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        adminService.logout();
        return ResponseEntity.ok().build();
    }

    /**
     * 비밀번호 재설정 요청 (OTP 발송)
     * POST /api/admin/password/reset-request
     */
    @PostMapping("/password/reset-request")
    public ResponseEntity<?> requestPasswordReset(
            @RequestBody PasswordResetRequest request
    ) {
        adminService.requestPasswordReset(request);
        return ResponseEntity.ok().build();
    }

    /**
     * OTP 검증
     * POST /api/admin/password/verify-otp
     */
    @PostMapping("/password/verify-otp")
    public ResponseEntity<?> passwordVerifyOtp(
            @RequestBody PasswordOtpVerifyRequest request
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
            @RequestBody PasswordResetConfirmRequest request
    ) {
        adminService.resetPassword(request);
        return ResponseEntity.ok().build();
    }

    /**
     * 비밀번호 변경 (로그인 상태)
     * PUT /api/admin/password
     */
    @PutMapping("/password")
    public ResponseEntity<?> changePassword(
            @RequestBody PasswordChangeRequest request,
            @AuthenticationPrincipal Admin admin
    ) {
        if (admin == null) {
            return ResponseEntity.status(401).build(); // 인증 필요
        }

        adminService.changePassword(request);
        return ResponseEntity.ok().build();
    }

    /**
     * Access 토큰 재발급
     * PUT /api/admin/refresh
     */
    @Operation(summary = "Access 토큰 재발급", description = "Access 토큰이 만료되는 경우 Refresh 토큰을 사용하여 새로운 Access 토큰을 발급받습니다.")
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@RequestBody RefreshTokenRequest request) {
        TokenResponse tokenResponse = adminService.refresh(request);
        return ResponseEntity.ok(tokenResponse);
    }

    /**
     * 내 정보 조회
     * 현재 로그인한 관리자의 상세 정보를 조회합니다.
     */
    @Operation(summary = "내 정보 조회", description = "현재 로그인한 관리자의 상세 정보를 조회합니다.")
    @GetMapping("/profile")
    public ResponseEntity<AdminInfoResponse> getMyInfo(
            @AuthenticationPrincipal String adminId
    ) {
        if (adminId == null) {
            return ResponseEntity.status(401).build();
        }

        AdminInfoResponse response =
                adminService.getAdminInfoById(Long.parseLong(adminId));

        return ResponseEntity.ok(response);
    }


    /**
     * 내 아파트 정보 조회
     * 현재 로그인한 관리자가 관리하는 아파트 단지의 정보를 조회합니다.
     */
    @Operation(summary = "내 아파트 정보 조회", description = "현재 로그인한 관리자가 관리하는 아파트 단지의 정보를 조회합니다.")
    @GetMapping("/apartment")
    public ResponseEntity<AdminApartmentResponse> getMyApartmentInfo(@AuthenticationPrincipal Admin admin) {
        if (admin == null) {
            return ResponseEntity.status(401).build(); // 인증 필요
        }

        // loginId 기준으로 안전하게 조회
        AdminApartmentResponse response = adminService.getAdminApartmentInfo(admin.getLoginId());
        return ResponseEntity.ok(response);
    }
}
