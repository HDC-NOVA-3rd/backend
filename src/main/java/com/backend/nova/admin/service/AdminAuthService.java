package com.backend.nova.admin.service;

import com.backend.nova.admin.dto.*;
import com.backend.nova.admin.entity.Admin;
import com.backend.nova.admin.entity.AdminMfaOtp;
import com.backend.nova.admin.entity.AdminStatus;
import com.backend.nova.admin.entity.OtpPurpose;
import com.backend.nova.admin.repository.AdminMfaOtpRepository;
import com.backend.nova.admin.repository.AdminRepository;
import com.backend.nova.auth.jwt.JwtProvider;
import com.backend.nova.global.exception.BusinessException;
import com.backend.nova.global.exception.ErrorCode;
import com.backend.nova.member.dto.TokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminAuthService {

    private final AdminRepository adminRepository;
    private final AdminMfaOtpRepository otpRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final JwtProvider jwtProvider;

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int OTP_EXPIRE_MINUTES = 5;

    /* ================= 관리자 회원가입 ================= */
    public void createAdmin(AdminCreateRequest request) {

        if (adminRepository.findByLoginId(request.loginId()).isPresent()) {
            throw new BusinessException(ErrorCode.ADMIN_LOGIN_ID_DUPLICATED);
        }

        Admin admin = Admin.builder()
                .loginId(request.loginId())
                .passwordHash(passwordEncoder.encode(request.password()))
                .name(request.name())
                .email(request.email())
                .role(request.role())
                .status(AdminStatus.ACTIVE)
                .build();

        adminRepository.save(admin);
    }

    /* ================= 관리자 로그인 ================= */
    public AdminLoginResponse login(AdminLoginRequest request) {
        Admin admin = getAdminByLoginId(request.loginId());

        validateAdminStatus(admin);

        if (!passwordEncoder.matches(request.password(), admin.getPasswordHash())) {
            handleLoginFailure(admin);
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        handleLoginSuccess(admin);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                admin.getId().toString(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        TokenResponse tokenResponse = jwtProvider.generateToken(authentication);

        return new AdminLoginResponse(
                admin.getId(),
                admin.getName(),
                tokenResponse.accessToken(),
                tokenResponse.refreshToken()
        );
    }

    /* ================= OTP 로그인 ================= */
    public void sendLoginOtp(String loginId) {
        Admin admin = getAdminByLoginId(loginId);
        validateAdminStatus(admin);
        sendOtp(admin, OtpPurpose.LOGIN);
    }

    public AdminLoginResponse verifyLoginOtp(SuperAdminLoginRequest request) {
        Admin admin = getAdminByLoginId(request.loginId());
        AdminMfaOtp otp = getLatestOtp(admin, OtpPurpose.LOGIN);
        validateOtp(otp, request.otpCode());
        markOtpVerified(otp);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                admin.getId().toString(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        TokenResponse tokenResponse = jwtProvider.generateToken(authentication);

        return new AdminLoginResponse(
                admin.getId(),
                admin.getName(),
                tokenResponse.accessToken(),
                tokenResponse.refreshToken()
        );
    }

    /* ================= 비밀번호 재설정 ================= */
    public void requestPasswordReset(PasswordResetRequest request) {
        Admin admin = adminRepository
                .findByLoginIdAndEmail(request.loginId(), request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_NOT_FOUND));

        validateAdminStatus(admin);

        sendOtp(admin, OtpPurpose.PASSWORD_RESET);
    }

    public void passwordVerifyOtp(PasswordOtpVerifyRequest request) {
        Admin admin = getAdminByLoginId(request.loginId());
        AdminMfaOtp otp = getLatestOtp(admin, OtpPurpose.PASSWORD_RESET);
        validateOtp(otp, request.otp());
        markOtpVerified(otp);
    }

    public void resetPassword(PasswordResetConfirmRequest request) {
        Admin admin = getAdminByLoginId(request.loginId());

        boolean verified = otpRepository
                .existsByAdminAndPurposeAndVerifiedAtIsNotNull(
                        admin, OtpPurpose.PASSWORD_RESET
                );

        if (!verified) {
            throw new BusinessException(ErrorCode.OTP_NOT_VERIFIED);
        }

        admin.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        adminRepository.save(admin);
    }

    public void changePassword(PasswordChangeRequest request) {
        Admin admin = getCurrentAdmin();

        if (!passwordEncoder.matches(request.currentPassword(), admin.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        admin.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        adminRepository.save(admin);
    }

    public void logout() {
        // JWT 기반 로그아웃 처리 시 클라이언트에서 토큰 삭제
    }

    /* ================= 내부 헬퍼 ================= */
    private Admin getAdminByLoginId(String loginId) {
        return adminRepository.findByLoginId(loginId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_NOT_FOUND));
    }

    private void validateAdminStatus(Admin admin) {
        if (admin.getStatus() != AdminStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.ADMIN_INACTIVE);
        }

        if (admin.getLockedUntil() != null &&
                admin.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.ADMIN_LOCKED);
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
                .findTopByAdminAndPurposeAndVerifiedAtIsNullOrderByCreatedAtDesc(admin, purpose)
                .orElseThrow(() -> new BusinessException(ErrorCode.OTP_NOT_FOUND));
    }

    private void validateOtp(AdminMfaOtp otp, String inputOtp) {
        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.OTP_EXPIRED);
        }

        if (otp.getAttemptCount() >= 5) {
            throw new BusinessException(ErrorCode.OTP_MAX_ATTEMPTS);
        }

        if (!otp.getOtpCode().equals(inputOtp)) {
            otp.increaseAttempt();
            otpRepository.save(otp);
            throw new BusinessException(ErrorCode.OTP_INVALID);
        }
    }

    private void markOtpVerified(AdminMfaOtp otp) {
        otp.markVerified();
        otpRepository.save(otp);
    }

    private String generateOtp() {
        return String.format("%06d", new SecureRandom().nextInt(1_000_000));
    }

    private Admin getCurrentAdmin() {
        String adminIdStr = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long adminId = Long.parseLong(adminIdStr);
        return adminRepository.findById(adminId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_NOT_FOUND));
    }
}
