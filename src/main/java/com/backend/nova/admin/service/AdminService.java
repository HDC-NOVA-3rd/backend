package com.backend.nova.admin.service;

import com.backend.nova.admin.dto.*;
import com.backend.nova.admin.entity.*;
import com.backend.nova.admin.repository.AdminRepository;
import com.backend.nova.apartment.entity.Apartment;
import com.backend.nova.apartment.repository.ApartmentRepository;
import com.backend.nova.auth.admin.AdminDetails;
import com.backend.nova.auth.jwt.JwtProvider;
import com.backend.nova.auth.jwt.JwtToken;
import com.backend.nova.auth.otp.StatelessOtpService;
import com.backend.nova.global.exception.BusinessException;
import com.backend.nova.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminService {

    private final AdminRepository adminRepository;
    private final ApartmentRepository apartmentRepository;
    private final StatelessOtpService otpService;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final JwtProvider jwtProvider;

    private static final int MAX_FAILED_ATTEMPTS = 5;

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

    /* ================= 일반 관리자 / 슈퍼관리자 로그인 ================= */
    public String login(AdminLoginRequest request) {

        Admin admin = adminRepository.findByLoginId(request.loginId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_LOGIN_FAILED));

        validateAdminStatus(admin);

        if (!passwordEncoder.matches(request.password(), admin.getPassword())) {
            handleLoginFailure(admin);
            throw new BusinessException(ErrorCode.ADMIN_LOGIN_FAILED);
        }

        handleLoginSuccess(admin);

        // 모든 관리자: OTP 발송
        String otp = otpService.generate(admin.getLoginId(), OtpPurpose.LOGIN);
        mailService.sendOtpMail(admin.getEmail(), otp);

        // OTP 입력 필요 메시지 반환
        return "OTP가 발송되었습니다. 이메일을 확인하세요.";
    }



    /* ================= 슈퍼관리자 OTP 검증 ================= */
    @Transactional
    public AdminTokenResponse loginVerifyOtp(AdminLoginConfirmRequest request) {

        Admin admin = adminRepository.findByLoginId(request.loginId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_NOT_FOUND));

        validateAdminStatus(admin);

        boolean verified = otpService.verify(
                admin.getLoginId(),
                OtpPurpose.LOGIN,
                request.otpCode()
        );

        if (!verified) {
            throw new BusinessException(ErrorCode.OTP_INVALID);
        }

        return issueToken(admin);
    }

    /* ================= 비밀번호 재설정 ================= */
    public void requestPasswordReset(AdminPasswordResetRequest request) {

        Admin admin = adminRepository
                .findByLoginIdAndEmail(request.loginId(), request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_NOT_FOUND));

        validateAdminStatus(admin);

        String otp = otpService.generate(
                admin.getLoginId(),
                OtpPurpose.PASSWORD_RESET
        );

        mailService.sendOtpMail(admin.getEmail(), otp);
    }

    public void passwordVerifyOtp(AdminPasswordChangeRequest request) {

        Admin admin = getAdminByLoginId(request.loginId());

        boolean verified = otpService.verify(
                admin.getLoginId(),
                OtpPurpose.PASSWORD_RESET,
                request.otpCode()
        );

        if (!verified) {
            throw new BusinessException(ErrorCode.OTP_INVALID);
        }
    }

    public void resetPassword(AdminPasswordResetConfirmRequest request) {

        Admin admin = getAdminByLoginId(request.loginId());

        boolean verified = otpService.verify(
                admin.getLoginId(),
                OtpPurpose.PASSWORD_RESET,
                request.otpCode()
        );

        if (!verified) {
            throw new BusinessException(ErrorCode.OTP_INVALID);
        }

        admin.setPassword(passwordEncoder.encode(request.newPassword()));
        adminRepository.save(admin);
    }

    /* ================= 비밀번호 변경 (로그인 상태) ================= */
    public void changePassword(AdminPasswordChangeConfirmRequest request, AdminDetails adminDetails) {

        Admin admin = adminRepository.findById(adminDetails.getAdminId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_NOT_FOUND));

        if (!passwordEncoder.matches(request.currentPassword(), admin.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        admin.setPassword(passwordEncoder.encode(request.newPassword()));
        adminRepository.save(admin);
    }

    /* ================= Access Token 재발급 ================= */
    public AdminTokenResponse refresh(AdminRefreshTokenRequest request) {

        Authentication auth =
                jwtProvider.getAuthenticationFromRefreshToken(request.refreshToken());

        JwtToken jwtToken = jwtProvider.generateToken(auth);

        AdminDetails adminDetails = (AdminDetails) auth.getPrincipal();
        Admin admin = adminRepository.findById(adminDetails.getAdminId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_NOT_FOUND));

        return AdminTokenResponse.builder()
                .accessToken(jwtToken.accessToken())
                .refreshToken(jwtToken.refreshToken())
                .adminId(admin.getId())
                .name(admin.getName())
                .role(admin.getRole().name())
                .build();
    }

    /* ================= 조회 ================= */
    public AdminInfoResponse getAdminInfo(AdminDetails adminDetails) {

        Admin admin = adminRepository.findById(adminDetails.getAdminId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_NOT_FOUND));

        return new AdminInfoResponse(
                admin.getId(),
                admin.getLoginId(),
                admin.getName(),
                admin.getEmail(),
                admin.getPhoneNumber(),
                admin.getBirthDate(),
                admin.getProfileImg(),
                admin.getRole().name(),
                admin.getApartment() != null ? admin.getApartment().getId() : null
        );
    }

    public AdminApartmentResponse getAdminApartmentInfo(AdminDetails adminDetails) {

        Admin admin = adminRepository.findById(adminDetails.getAdminId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_NOT_FOUND));

        var apartment = admin.getApartment();
        if (apartment == null) return null;

        return new AdminApartmentResponse(
                apartment.getId(),
                apartment.getName(),
                apartment.getAddress()
        );
    }

    /* ================= 내부 헬퍼 ================= */
    private AdminTokenResponse issueToken(Admin admin) {

        AdminDetails adminDetails = new AdminDetails(admin);
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        adminDetails,
                        null,
                        adminDetails.getAuthorities()
                );

        JwtToken jwtToken = jwtProvider.generateToken(authentication);

        return AdminTokenResponse.builder()
                .accessToken(jwtToken.accessToken())
                .refreshToken(jwtToken.refreshToken())
                .adminId(admin.getId())
                .name(admin.getName())
                .role(admin.getRole().name())
                .build();
    }

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

    private Admin getCurrentAdmin() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                !(authentication.getPrincipal() instanceof AdminDetails adminDetails)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return adminRepository.findById(adminDetails.getAdminId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_NOT_FOUND));
    }

    public void logout(AdminDetails adminDetails) {
        // JWT blacklist / refresh token 무효화 등 필요 시 구현
    }
}
