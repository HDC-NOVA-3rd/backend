package com.backend.nova.admin.controller;

import com.backend.nova.admin.dto.*;
import com.backend.nova.admin.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
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
            @RequestBody @Valid AdminLoginConfirmRequest request,
            HttpServletResponse response // 쿠키 설정을 위해 추가
    ) {
        // 서비스에서 토큰 생성 및 쿠키 설정 로직 수행
        AdminTokenResponse tokenResponse = adminService.loginVerifyOtp(request, response);
        return ResponseEntity.ok(tokenResponse);
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
            @CookieValue(name = "refreshToken") String refreshToken, // 쿠키에서 직접 읽음
            HttpServletResponse response
    ) {
        return ResponseEntity.ok(adminService.refresh(refreshToken, response));
    }

}
