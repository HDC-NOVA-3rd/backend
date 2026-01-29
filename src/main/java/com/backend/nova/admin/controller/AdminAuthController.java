package com.backend.nova.admin.controller;

import com.backend.nova.admin.dto.*;
import com.backend.nova.admin.service.AdminAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;


    /**
     * 관리자 회원가입 (슈퍼관리자만 가능)
     * POST /api/admin
     */
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> createAdmin(
            @RequestBody AdminCreateRequest request
    ) {
        adminAuthService.createAdmin(request);
        return ResponseEntity.ok().build();
    }


    /**
     * 관리자 로그인
     * POST /api/admin/login
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AdminLoginRequest request) {
        AdminLoginResponse response = adminAuthService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 슈퍼관리자 로그인시 otp인증
     * POST /api/admin/login/verify-otp
     */
//    @PostMapping("/login/verify-otp")
//    @PreAuthorize("hasRole('SUPER_ADMIN')")
//    public ResponseEntity<?> loginVerifyOtp(@RequestBody AdminLoginOtpVerifyRequest request) {
//        AdminLoginResponse response = adminAuthService.loginVerifyOtp(request);
//        return ResponseEntity.ok(response);
//    }

    /**
     * 관리자 로그아웃
     * POST /api/admin/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        adminAuthService.logout();
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
        adminAuthService.requestPasswordReset(request);
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
        adminAuthService.passwordVerifyOtp(request);
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
        adminAuthService.resetPassword(request);
        return ResponseEntity.ok().build();
    }

    /**
     * 비밀번호 변경 (로그인 상태)
     * PUT /api/admin/password
     */
    @PutMapping("/password")
    public ResponseEntity<?> changePassword(
            @RequestBody PasswordChangeRequest request
    ) {
        adminAuthService.changePassword(request);
        return ResponseEntity.ok().build();
    }
}
