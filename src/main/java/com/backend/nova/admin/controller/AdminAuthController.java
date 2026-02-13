package com.backend.nova.admin.controller;

import com.backend.nova.admin.dto.*;
import com.backend.nova.admin.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin-Auth", description = "관리자 로그인 API")
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminService adminService;

    /**
     * 관리자 로그인
     */
    @PostMapping("/login")
    public ResponseEntity<AdminMessageResponse> login(
            @RequestBody @Valid AdminLoginRequest request
    ) {
        return ResponseEntity.ok(adminService.login(request));
    }

    /**
     * 로그인 OTP 검증
     */
    @PostMapping("/login/otp")
    public ResponseEntity<AdminTokenResponse> loginVerifyOtp(
            @RequestBody @Valid AdminLoginConfirmRequest request
    ) {
        return ResponseEntity.ok(adminService.loginVerifyOtp(request));
    }



    /**
     * 미로그인 비밀번호 초기화 요청 (OTP 발송)
     */
    @PostMapping("/password/reset/request")
    public ResponseEntity<AdminMessageResponse> requestPasswordReset(
            @RequestBody @Valid AdminPasswordResetRequest request
    ) {
        return ResponseEntity.ok(adminService.requestResetPassword(request));
    }

    /**
     * 미로그인 비밀번호 초기화
     */
    @PostMapping("/password/reset/confirm")
    public ResponseEntity<AdminMessageResponse> resetPassword(
            @RequestBody @Valid AdminPasswordResetConfirmRequest request
    ) {
        return ResponseEntity.ok(adminService.confirmResetPassword(request));
    }

    /**
     * Access 토큰 재발급 토큰 만료 시 접근성 확보
     */
    @Operation(summary = "Access 토큰 재발급", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/refresh")
    public ResponseEntity<AdminTokenResponse> refresh(
            @RequestBody AdminRefreshTokenRequest request
    ) {
        return ResponseEntity.ok(adminService.refresh(request));
    }

}
