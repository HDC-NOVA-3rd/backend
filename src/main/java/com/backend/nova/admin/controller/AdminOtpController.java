package com.backend.nova.admin.controller;

import com.backend.nova.admin.service.AdminService;
import com.backend.nova.auth.admin.AdminDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/device")
//@RequestMapping("/api/admin/otp")
@RequiredArgsConstructor
public class AdminOtpController {

    private final AdminService adminService;

    @PostMapping("/setup")
    public ResponseEntity<OtpSetupResponse> setup(
            @AuthenticationPrincipal AdminDetails adminDetails
    ) {
        return ResponseEntity.ok(adminService.setupOtp(adminDetails));
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verify(
            @RequestBody OtpVerifyRequest request,
            @AuthenticationPrincipal AdminDetails adminDetails
    ) {
        adminService.verifyOtp(request, adminDetails);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/status")
    public ResponseEntity<OtpStatusResponse> status(
            @AuthenticationPrincipal AdminDetails adminDetails
    ) {
        return ResponseEntity.ok(adminService.getOtpStatus(adminDetails));
    }
}
