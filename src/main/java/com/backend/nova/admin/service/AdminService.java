package com.backend.nova.admin.service;

import com.backend.nova.admin.dto.*;
import com.backend.nova.admin.entity.*;
import com.backend.nova.admin.repository.AdminMfaOtpRepository;
import com.backend.nova.admin.repository.AdminRepository;
import com.backend.nova.apartment.entity.Apartment;
import com.backend.nova.apartment.repository.ApartmentRepository;
import com.backend.nova.auth.admin.AdminDetails;
import com.backend.nova.auth.jwt.JwtProvider;
import com.backend.nova.global.exception.BusinessException;
import com.backend.nova.global.exception.ErrorCode;
import com.backend.nova.member.dto.RefreshTokenRequest;
import com.backend.nova.member.dto.TokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminService {

    private final AdminRepository adminRepository;
    private final ApartmentRepository apartmentRepository;
    private final AdminMfaOtpRepository otpRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final JwtProvider jwtProvider;

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int OTP_EXPIRE_MINUTES = 5;

    /* ================= 관리자 회원가입 ================= */
    public void createAdmin(AdminCreateRequest request) {
        Admin currentAdmin = getCurrentAdmin();

        if (adminRepository.findByLoginId(request.loginId()).isPresent()) {
            throw new BusinessException(ErrorCode.ADMIN_LOGIN_ID_DUPLICATED);
        }

        if (adminRepository.findByEmail(request.email()).isPresent()) {
            throw new BusinessException(ErrorCode.ADMIN_EMAIL_DUPLICATED);
        }

        Apartment currentApartment = currentAdmin.getApartment();
        if (currentApartment == null) {
            throw new BusinessException(ErrorCode.APARTMENT_NOT_FOUND);
        }

        Admin admin = Admin.builder()
                .loginId(request.loginId())
                .password(passwordEncoder.encode(request.password()))
                .name(request.name())
                .email(request.email())
                .role(request.role() != null ? request.role() : AdminRole.ADMIN)
                .status(AdminStatus.ACTIVE)
                .apartment(currentApartment)
                .build();

        adminRepository.save(admin);
    }

    /* ================= 관리자 로그인 ================= */
    public TokenResponse login(AdminLoginRequest request) {
        Admin admin = adminRepository.findByLoginId(request.loginId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_LOGIN_FAILED));

        validateAdminStatus(admin);

        if (!passwordEncoder.matches(request.password(), admin.getPassword())) {
            handleLoginFailure(admin);
            throw new BusinessException(ErrorCode.ADMIN_LOGIN_FAILED);
        }

        handleLoginSuccess(admin);

        // AdminDetails 생성
        AdminDetails adminDetails = new AdminDetails(admin);

        // Authentication 객체 생성
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        adminDetails,
                        null,
                        adminDetails.getAuthorities()
                );

        // JWT 발급 (공용 TokenResponse 사용)
        return jwtProvider.generateToken(authentication);
    }

    /* ================= OTP 로그인 ================= */
    public TokenResponse verifyLoginOtp(SuperAdminLoginRequest request) {
        Admin admin = getAdminByLoginId(request.loginId());

        AdminMfaOtp otp = getLatestOtp(admin, OtpPurpose.LOGIN);
        validateOtp(otp, request.otpCode());
        markOtpVerified(otp);

        AdminDetails adminDetails = new AdminDetails(admin);

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        adminDetails,
                        null,
                        adminDetails.getAuthorities()
                );

        return jwtProvider.generateToken(authentication);
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

        admin.setPassword(passwordEncoder.encode(request.newPassword()));
        adminRepository.save(admin);
    }

    public void changePassword(PasswordChangeRequest request) {
        Admin admin = getCurrentAdmin();

        if (!passwordEncoder.matches(request.currentPassword(), admin.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        admin.setPassword(passwordEncoder.encode(request.newPassword()));
        adminRepository.save(admin);
    }

    /* ================= Access Token 재발급 ================= */
    public TokenResponse refresh(RefreshTokenRequest request) {
        // 1. Refresh Token → Authentication
        Authentication auth = jwtProvider.getAuthenticationFromRefreshToken(request.refreshToken());

        // 2. JWT 재발급
        return jwtProvider.generateToken(auth);
    }

    /* ================= 내부 헬퍼 ================= */
    private Admin getAdminByLoginId(String loginId) {
        return adminRepository.findByLoginId(loginId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_NOT_FOUND));
    }

    private void validateAdminStatus(Admin admin) {
        if (admin.getLockedUntil() != null &&
                admin.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.ADMIN_LOCKED);
        }

        if (admin.getStatus() != AdminStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.ADMIN_INACTIVE);
        }
    }

    private void handleLoginFailure(Admin admin) {
        int count = admin.getFailedLoginCount() + 1;
        admin.setFailedLoginCount(count);

        if (count >= MAX_FAILED_ATTEMPTS) {
            admin.setLockedUntil(LocalDateTime.now().plusMinutes(30));
            admin.setFailedLoginCount(0);
        }
    }

    private void handleLoginSuccess(Admin admin) {
        admin.setFailedLoginCount(0);
        admin.setLockedUntil(null);
        admin.setLastLoginAt(LocalDateTime.now());
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

    /* ================= 현재 로그인 관리자 ================= */
    private Admin getCurrentAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof AdminDetails adminDetails)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return adminRepository.findById(adminDetails.getAdminId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_NOT_FOUND));
    }
}
