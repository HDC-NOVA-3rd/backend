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
import com.backend.nova.member.dto.RefreshTokenRequest;
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

    /* ================= 관리자 생성 ================= */
    public void createAdmin(AdminCreateRequest request) {
        Admin currentAdmin = getCurrentAdmin();

        if (adminRepository.findByLoginId(request.loginId()).isPresent()) {
            throw new BusinessException(ErrorCode.ADMIN_LOGIN_ID_DUPLICATED);
        }

        if (adminRepository.findByEmail(request.email()).isPresent()) {
            throw new BusinessException(ErrorCode.ADMIN_EMAIL_DUPLICATED);
        }

        Apartment apartment = currentAdmin.getApartment();
        if (apartment == null) {
            throw new BusinessException(ErrorCode.APARTMENT_NOT_FOUND);
        }

        Admin admin = Admin.builder()
                .loginId(request.loginId())
                .password(passwordEncoder.encode(request.password()))
                .name(request.name())
                .email(request.email())
                .role(request.role() != null ? request.role() : AdminRole.ADMIN)
                .status(AdminStatus.ACTIVE)
                .apartment(apartment)
                .build();

        adminRepository.save(admin);
    }

    /* ================= 로그인 ================= */
    public String login(AdminLoginRequest request) {

        Admin admin = adminRepository.findByLoginId(request.loginId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_LOGIN_FAILED));

        validateAdminStatus(admin);

        if (!passwordEncoder.matches(request.password(), admin.getPassword())) {
            handleLoginFailure(admin);
            throw new BusinessException(ErrorCode.ADMIN_LOGIN_FAILED);
        }

        handleLoginSuccess(admin);

        String otp = otpService.generate(admin.getLoginId(), OtpPurpose.LOGIN);
        mailService.sendOtpMail(admin.getEmail(), otp);

        return "OTP가 발송되었습니다. 이메일을 확인하세요.";
    }

    /* ================= 로그인 OTP 검증 ================= */
    public AdminTokenResponse loginVerifyOtp(AdminLoginConfirmRequest request) {

        Admin admin = getAdminByLoginId(request.loginId());
        validateAdminStatus(admin);

        if (!otpService.verify(admin.getLoginId(), OtpPurpose.LOGIN, request.otpCode())) {
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

        String otp = otpService.generate(admin.getLoginId(), OtpPurpose.PASSWORD_RESET);
        mailService.sendOtpMail(admin.getEmail(), otp);
    }

    public void resetPassword(AdminPasswordResetConfirmRequest request) {

        Admin admin = getAdminByLoginId(request.loginId());

        if (!otpService.verify(admin.getLoginId(), OtpPurpose.PASSWORD_RESET, request.otpCode())) {
            throw new BusinessException(ErrorCode.OTP_INVALID);
        }

        admin.setPassword(passwordEncoder.encode(request.newPassword()));
    }

    /* ================= 비밀번호 변경 OTP 검증 ================= */
    public void passwordVerifyOtp(AdminPasswordChangeRequest request) {

        Admin admin = getAdminByLoginId(request.loginId());

        if (!otpService.verify(admin.getLoginId(), OtpPurpose.PASSWORD_RESET, request.otpCode())) {
            throw new BusinessException(ErrorCode.OTP_INVALID);
        }
    }

    /* ================= 비밀번호 변경 ================= */
    public void changePassword(
            AdminPasswordChangeConfirmRequest request,
            AdminDetails adminDetails
    ) {
        Admin admin = adminRepository.findById(adminDetails.getAdmin().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_NOT_FOUND));

        validateAdminStatus(admin);
        admin.setPassword(passwordEncoder.encode(request.newPassword()));
    }

    /* ================= 토큰 재발급 ================= */
    public AdminTokenResponse refresh(RefreshTokenRequest request) {

        if (!jwtProvider.validateToken(request.refreshToken())) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        Admin admin = getAdminByLoginId(jwtProvider.getSubject(request.refreshToken()));
        validateAdminStatus(admin);

        return issueToken(admin);
    }

    /* ================= 내부 ================= */

    private AdminTokenResponse issueToken(Admin admin) {

        AdminDetails adminDetails = new AdminDetails(admin);

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        adminDetails,
                        null,
                        adminDetails.getAuthorities()
                );

        JwtToken token = jwtProvider.generateToken(authentication);

        return AdminTokenResponse.builder()
                .accessToken(token.accessToken())
                .refreshToken(token.refreshToken())
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
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                !(authentication.getPrincipal() instanceof AdminDetails adminDetails)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return adminRepository.findById(adminDetails.getAdmin().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_NOT_FOUND));
    }

    public void logout(AdminDetails adminDetails) {
        // TODO refresh token blacklist
    }

    public AdminInfoResponse getAdminInfo(AdminDetails adminDetails) {
        return null;
    }

    public AdminApartmentResponse getAdminApartmentInfo(AdminDetails adminDetails) {
        return null;
    }
}
