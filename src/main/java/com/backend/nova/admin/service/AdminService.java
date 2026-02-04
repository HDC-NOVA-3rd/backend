package com.backend.nova.admin.service;

import com.backend.nova.admin.dto.*;
import com.backend.nova.admin.entity.*;
import com.backend.nova.admin.repository.AdminMfaOtpRepository;
import com.backend.nova.admin.repository.AdminRepository;
import com.backend.nova.apartment.entity.Apartment;
import com.backend.nova.apartment.repository.ApartmentRepository;
import com.backend.nova.auth.admin.AdminDetails;
import com.backend.nova.auth.jwt.JwtProvider;
import com.backend.nova.auth.jwt.JwtToken;
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
        // 로그인 아이디로 관리자 조회
        Admin admin = adminRepository.findByLoginId(request.loginId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_LOGIN_FAILED));

        // 계정 상태 검증 (잠김, 비활성 등)
        validateAdminStatus(admin);

        // 비밀번호 검증
        if (!passwordEncoder.matches(request.password(), admin.getPassword())) {
            handleLoginFailure(admin);
            throw new BusinessException(ErrorCode.ADMIN_LOGIN_FAILED);
        }

        // 로그인 성공 처리 (실패 카운트 초기화 등)
        handleLoginSuccess(admin);

        // 슈퍼관리자인 경우 OTP 발급 후 JWT는 발급하지 않음
        if (admin.getRole() == AdminRole.SUPER_ADMIN) {
            sendOtp(admin, OtpPurpose.LOGIN);
            // 여기서 바로 JWT를 발급하지 않고, OTP 입력 후 검증을 요구
            throw new BusinessException(ErrorCode.SUPER_ADMIN_OTP_REQUIRED);
            // 또는 별도 DTO 반환 가능
        }

        // 일반 관리자 로그인: JWT 발급
        AdminDetails adminDetails = new AdminDetails(admin);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                adminDetails,
                null,
                adminDetails.getAuthorities()
        );

        JwtToken jwtToken = jwtProvider.generateToken(authentication);

        return TokenResponse.builder()
                .accessToken(jwtToken.accessToken())
                .refreshToken(jwtToken.refreshToken())
                .id(admin.getId())
                .loginId(admin.getLoginId())
                .name(admin.getName())
                .role(admin.getRole().name())
                .build();
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

    public void changePassword(PasswordChangeRequest request, AdminDetails adminDetails) {
        Admin admin = adminRepository.findById(adminDetails.getAdminId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_NOT_FOUND));

        if (!passwordEncoder.matches(request.currentPassword(), admin.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        admin.setPassword(passwordEncoder.encode(request.newPassword()));
        adminRepository.save(admin);
    }

    /* ================= Access Token 재발급 ================= */
    public TokenResponse refresh(RefreshTokenRequest request) {
        Authentication auth = jwtProvider.getAuthenticationFromRefreshToken(request.refreshToken());
        JwtToken jwtToken = jwtProvider.generateToken(auth);

        AdminDetails adminDetails = (AdminDetails) auth.getPrincipal();
        Admin admin = adminRepository.findById(adminDetails.getAdminId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_NOT_FOUND));

        return TokenResponse.builder()
                .accessToken(jwtToken.accessToken())
                .refreshToken(jwtToken.refreshToken())
                .id(admin.getId())
                .loginId(admin.getLoginId())
                .name(admin.getName())
                .role(admin.getRole().name())
                .build();
    }

    /* ================= AdminDetails 기반 조회 ================= */
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
    private Admin getAdminByLoginId(String loginId) {
        return adminRepository.findByLoginId(loginId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_NOT_FOUND));
    }

    private void validateAdminStatus(Admin admin) {
        if (admin.getLockedUntil() != null && admin.getLockedUntil().isAfter(LocalDateTime.now())) {
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
        String otpCode = String.format("%06d", new SecureRandom().nextInt(1_000_000));

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

    private Admin getCurrentAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof AdminDetails adminDetails)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return adminRepository.findById(adminDetails.getAdminId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_NOT_FOUND));
    }

    public void logout(AdminDetails adminDetails) {
        // 필요한 로그아웃 로직 (JWT blacklist 등) 구현 가능
    }

    @Transactional
    public TokenResponse loginVerifyOtp(SuperAdminLoginRequest request) {
        // 1. 관리자 조회
        Admin admin = adminRepository.findByLoginId(request.loginId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_NOT_FOUND));

        // 2. 상태 체크
        validateAdminStatus(admin);

        // 3. OTP 조회 (LOGIN 용도)
        AdminMfaOtp otp = otpRepository
                .findTopByAdminAndPurposeAndVerifiedAtIsNullOrderByCreatedAtDesc(admin, OtpPurpose.LOGIN)
                .orElseThrow(() -> new BusinessException(ErrorCode.OTP_NOT_FOUND));

        // 4. OTP 검증
        if (otp.isExpired()) throw new BusinessException(ErrorCode.OTP_EXPIRED);
        if (otp.getAttemptCount() >= 5) throw new BusinessException(ErrorCode.OTP_MAX_ATTEMPTS);
        if (!otp.getOtpCode().equals(request.otpCode())) {
            otp.increaseAttempt();
            otpRepository.save(otp);
            throw new BusinessException(ErrorCode.OTP_INVALID);
        }

        // 5. 검증 완료 표시
        otp.markVerified();
        otpRepository.save(otp);

        // 6. JWT 발급
        AdminDetails adminDetails = new AdminDetails(admin);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                adminDetails,
                null,
                adminDetails.getAuthorities()
        );
        JwtToken jwtToken = jwtProvider.generateToken(authentication);

        // 7. TokenResponse 반환
        return TokenResponse.builder()
                .accessToken(jwtToken.accessToken())
                .refreshToken(jwtToken.refreshToken())
                .id(admin.getId())
                .loginId(admin.getLoginId())
                .name(admin.getName())
                .role(admin.getRole().name())
                .build();
    }

}
