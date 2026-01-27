package com.backend.nova.admin.service;

import com.backend.nova.admin.dto.*;
import com.backend.nova.admin.entity.Admin;
import com.backend.nova.admin.entity.AdminMfaOtp;
import com.backend.nova.admin.entity.AdminStatus;
import com.backend.nova.admin.entity.OtpPurpose;
import com.backend.nova.admin.repository.AdminMfaOtpRepository;
import com.backend.nova.admin.repository.AdminRepository;
import com.backend.security.jwt.AdminJwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminAuthService {

    private final AdminRepository adminRepository;
    private final AdminMfaOtpRepository otpRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final AdminJwtTokenProvider jwtTokenProvider;

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int OTP_EXPIRE_MINUTES = 5;

    /* ================= 로그인 ================= */

    public AdminLoginResponse login(AdminLoginRequest request) {
        Admin admin = getAdminByLoginId(request.getLoginId());

        validateAdminStatus(admin);

        if (!passwordEncoder.matches(request.getPassword(), admin.getPasswordHash())) {
            handleLoginFailure(admin);
            throw new IllegalArgumentException("비밀번호 불일치");
        }

        handleLoginSuccess(admin);

        sendOtp(admin, OtpPurpose.LOGIN);

        return new AdminLoginResponse(admin.getId(), admin.getName(), null);
    }

    public String verifyOtp(AdminOtpVerifyRequest request) {
        Admin admin = getAdminByLoginId(request.getLoginId());

        AdminMfaOtp otp = getLatestOtp(admin, OtpPurpose.LOGIN);

        validateOtp(otp, request.getOtpCode());

        markOtpVerified(otp);

        return jwtTokenProvider.createToken(admin.getId(), admin.getLoginId());
    }

    /* ================= 비밀번호 재설정 ================= */

    public void requestPasswordReset(PasswordResetRequest request) {
        Admin admin = adminRepository
                .findByLoginIdAndEmail(request.getLoginId(), request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("관리자 정보 없음"));

        validateAdminStatus(admin);

        sendOtp(admin, OtpPurpose.PASSWORD_RESET);
    }

    public void verifyOtp(PasswordOtpVerifyRequest request) {
        Admin admin = getAdminByLoginId(request.getLoginId());

        AdminMfaOtp otp = getLatestOtp(admin, OtpPurpose.PASSWORD_RESET);

        validateOtp(otp, request.getOtp());

        markOtpVerified(otp);
    }

    public void resetPassword(PasswordResetConfirmRequest request) {
        Admin admin = getAdminByLoginId(request.getLoginId());

        boolean verified = otpRepository
                .existsByAdminAndPurposeAndVerifiedAtIsNotNull(
                        admin, OtpPurpose.PASSWORD_RESET
                );

        if (!verified) {
            throw new IllegalStateException("OTP 검증 필요");
        }

        admin.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        adminRepository.save(admin);
    }

    /* ================= 로그인 상태 ================= */

    public void changePassword(PasswordChangeRequest request) {
        Admin admin = getCurrentAdmin();

        if (!passwordEncoder.matches(
                request.getCurrentPassword(),
                admin.getPasswordHash()
        )) {
            throw new IllegalArgumentException("현재 비밀번호 불일치");
        }

        admin.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        adminRepository.save(admin);
    }

    public void logout() {
        // JWT → 클라이언트 토큰 삭제
    }

    /* ================= 내부 헬퍼 ================= */

    private Admin getAdminByLoginId(String loginId) {
        return adminRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("관리자 ID 없음"));
    }

    private void validateAdminStatus(Admin admin) {
        if (admin.getStatus() != AdminStatus.ACTIVE) {
            throw new IllegalStateException("계정 비활성 상태");
        }

        if (admin.getLockedUntil() != null &&
                admin.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new IllegalStateException("계정 잠금 상태");
        }
    }

    private void handleLoginFailure(Admin admin) {
        int count = admin.getFailedLoginCount() + 1;
        admin.setFailedLoginCount(count);

        if (count >= MAX_FAILED_ATTEMPTS) {
            admin.setLockedUntil(LocalDateTime.now().plusMinutes(30));
            admin.setFailedLoginCount(0);
        }

        adminRepository.save(admin);
    }

    private void handleLoginSuccess(Admin admin) {
        admin.setFailedLoginCount(0);
        admin.setLockedUntil(null);
        admin.setLastLoginAt(LocalDateTime.now());
        adminRepository.save(admin);
    }

    private void sendOtp(Admin admin, OtpPurpose purpose) {
        String otpCode = generateOtp();

        AdminMfaOtp otp = AdminMfaOtp.builder()
                .admin(admin)
                .otpCode(otpCode)
                .purpose(purpose)
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRE_MINUTES))
                .build();

        otpRepository.save(otp);
        mailService.sendOtpMail(admin.getEmail(), otpCode);
    }

    private AdminMfaOtp getLatestOtp(Admin admin, OtpPurpose purpose) {
        return otpRepository
                .findTopByAdminAndPurposeAndVerifiedAtIsNullOrderByCreatedAtDesc(
                        admin, purpose
                )
                .orElseThrow(() -> new IllegalArgumentException("OTP 없음"));
    }

    private void validateOtp(AdminMfaOtp otp, String inputOtp) {
        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("OTP 만료");
        }

        if (otp.getAttemptCount() >= 5) {
            throw new IllegalStateException("OTP 시도 횟수 초과");
        }

        if (!otp.getOtpCode().equals(inputOtp)) {
            otp.increaseAttempt();
            otpRepository.save(otp);
            throw new IllegalArgumentException("OTP 불일치");
        }
    }

    private void markOtpVerified(AdminMfaOtp otp) {
        otp.markVerified();
        otpRepository.save(otp);
    }

    private String generateOtp() {
        return String.format("%06d",
                new SecureRandom().nextInt(1_000_000));
    }

    private Admin getCurrentAdmin() {
        // SecurityContext에서 adminId 꺼내서 조회
        return null;
    }
}
