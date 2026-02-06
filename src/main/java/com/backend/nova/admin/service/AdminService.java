package com.backend.nova.admin.service;

import com.backend.nova.admin.dto.*;
import com.backend.nova.admin.entity.*;
import com.backend.nova.admin.repository.AdminDeviceRepository;
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
import com.backend.nova.member.dto.TokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminService {

    private final AdminRepository adminRepository;
    private final ApartmentRepository apartmentRepository;
    private final AdminDeviceRepository adminDeviceRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminMailService mailService;
    private final JwtProvider jwtProvider;
    private final StatelessOtpService otpService;
    private final AdminDeviceService adminDeviceService;

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
    public AdminLoginResponse login(AdminLoginRequest request, HttpServletRequest httpRequest) {
        Admin admin = adminRepository.findByLoginId(request.loginId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_LOGIN_FAILED));

        if (!passwordEncoder.matches(request.password(), admin.getPassword())) {
            throw new BusinessException(ErrorCode.ADMIN_LOGIN_FAILED);
        }

        // OTP 비활성 → 바로 로그인
        if (!admin.isOtpEnabled()) {
            String token = jwtProvider.createAdminAccessToken(
                    admin.getId(),
                    admin.getRole().getAuthority(),
                    request.deviceId()
            );
            return AdminLoginResponse.success(token);
        }

        // OTP 활성 → 생성 & 메일 발송
        String otp = otpService.generate(admin.getLoginId(), OtpPurpose.LOGIN);
        mailService.sendOtpMail(admin.getEmail(), otp);

        // Challenge Token 발급 (Stateless)
        String challengeToken = jwtProvider.createAdminChallengeToken(
                admin.getLoginId(),
                AdminChallengePurpose.LOGIN,
                Duration.ofMinutes(5)
        );

        return AdminLoginResponse.otpRequired(challengeToken);
    }

    public AdminLoginResponse verifyLoginOtp(AdminLoginVerifyOtpRequest request, HttpServletRequest httpRequest) {
        AdminChallengeToken challenge = jwtProvider.parseAdminChallengeToken(request.challengeToken());

        if (challenge.getPurpose() != AdminChallengePurpose.LOGIN) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        boolean verified = otpService.verify(challenge.getLoginId(), OtpPurpose.LOGIN, request.otp());
        if (!verified) {
            throw new BusinessException(ErrorCode.OTP_INVALID);
        }

        Admin admin = adminRepository.findByLoginId(challenge.getLoginId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_NOT_FOUND));

        adminDeviceService.registerDevice(admin, request.deviceId(), httpRequest.getRemoteAddr());

        String token = jwtProvider.createAdminAccessToken(
                admin.getId(),
                admin.getRole().getAuthority(),
                request.deviceId()
        );

        return AdminLoginResponse.success(token);
    }

    /* ================= 비밀번호 ================= */
    public void requestPasswordReset(AdminPasswordResetRequest request) {
        Admin admin = adminRepository.findByLoginIdAndEmail(
                request.loginId(), request.email()
        ).orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_NOT_FOUND));

        String otp = otpService.generate(admin.getLoginId(), OtpPurpose.PASSWORD_RESET);
        mailService.sendOtpMail(admin.getEmail(), otp);
    }


    public void passwordVerifyOtp(AdminPasswordOtpVerifyRequest request) {
        boolean verified = otpService.verify(
                request.loginId(),
                OtpPurpose.PASSWORD_RESET,
                request.otp()
        );

        if (!verified) {
            throw new BusinessException(ErrorCode.OTP_INVALID);
        }
    }


    public void resetPassword(AdminPasswordResetConfirmRequest request) {
        boolean verified = otpService.isVerified(
                request.loginId(),
                OtpPurpose.PASSWORD_RESET
        );

        if (!verified) {
            throw new BusinessException(ErrorCode.OTP_NOT_VERIFIED);
        }

        Admin admin = adminRepository.findByLoginId(request.loginId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_NOT_FOUND));

        admin.setPassword(passwordEncoder.encode(request.newPassword()));
    }


    public void changePassword(AdminPasswordChangeRequest request, AdminDetails adminDetails) {
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
        JwtToken token = jwtProvider.generateToken(auth);

        AdminDetails details = (AdminDetails) auth.getPrincipal();
        Admin admin = adminRepository.findById(details.getAdminId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_NOT_FOUND));

        return TokenResponse.from(admin, token);
    }

    /* ================= Admin 조회 ================= */
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

        Apartment apartment = admin.getApartment();
        if (apartment == null) return null;

        return new AdminApartmentResponse(
                apartment.getId(),
                apartment.getName(),
                apartment.getAddress()
        );
    }

    /* ================= 기기 관리 ================= */
    public List<AdminDeviceResponse> getMyDevices(AdminDetails adminDetails) {
        Admin admin = adminRepository.findById(adminDetails.getAdminId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_NOT_FOUND));

        return adminDeviceRepository.findByAdminAndRevokedAtIsNullOrderByCreatedAtDesc(admin)
                .stream()
                .map(d -> new AdminDeviceResponse(
                        d.getId(),
                        maskDeviceId(d.getDeviceId()),
                        d.isTrusted(),
                        d.getLastIp(),
                        d.getLastUsedAt(),
                        d.getCreatedAt()
                ))
                .toList();
    }

    public void revokeDevice(AdminDetails adminDetails, String deviceId, String ip) {
        Admin admin = adminRepository.findById(adminDetails.getAdminId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_NOT_FOUND));

        AdminDevice device = adminDeviceRepository
                .findByAdminAndDeviceIdAndRevokedAtIsNull(admin, deviceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_NOT_FOUND));

        device.setRevokedAt(LocalDateTime.now());
    }

    public void registerDevice(Admin admin, String deviceId, String ip) {
        adminDeviceService.registerDevice(admin, deviceId, ip);
    }

    /* ================= 내부 헬퍼 ================= */
    private Admin getCurrentAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AdminDetails adminDetails)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return adminRepository.findById(adminDetails.getAdminId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_NOT_FOUND));
    }

    private Admin getAdminByLoginId(String loginId) {
        return adminRepository.findByLoginId(loginId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_NOT_FOUND));
    }

    private void sendOtp(Admin admin, OtpPurpose purpose) {
        String otpCode = String.format("%06d", new SecureRandom().nextInt(1_000_000));
        AdminDevice otp = AdminDevice.builder()
                .admin(admin)
                .otpCode(otpCode)
                .purpose(purpose)
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRE_MINUTES))
                .build();
        adminDeviceRepository.save(otp);
        mailService.sendOtpMail(admin.getEmail(), otpCode);
    }

    private AdminDevice getLatestOtp(Admin admin, OtpPurpose purpose) {
        return adminDeviceRepository
                .findTopByAdminAndPurposeAndVerifiedAtIsNullOrderByCreatedAtDesc(admin, purpose)
                .orElseThrow(() -> new BusinessException(ErrorCode.OTP_NOT_FOUND));
    }

    private void validateOtp(AdminDevice otp, String inputOtp) {
        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.OTP_EXPIRED);
        }
        if (otp.getAttemptCount() >= 5) {
            throw new BusinessException(ErrorCode.OTP_MAX_ATTEMPTS);
        }
        if (!otp.getOtpCode().equals(inputOtp)) {
            otp.increaseAttempt();
            adminDeviceRepository.save(otp);
            throw new BusinessException(ErrorCode.OTP_INVALID);
        }
    }

    private void markOtpVerified(AdminDevice otp) {
        otp.markVerified();
        adminDeviceRepository.save(otp);
    }

    private String maskDeviceId(String deviceId) {
        if (deviceId.length() <= 4) return "****";
        return "****" + deviceId.substring(deviceId.length() - 4);
    }

    public void logout(AdminDetails adminDetails) {
        // JWT blacklist 등 로그아웃 로직 구현 가능
    }

    public AdminDeviceResponse registerCurrentDevice(AdminDetails adminDetails) {
        return null;
    }
}
