package com.backend.nova.admin.service;

import com.backend.nova.admin.dto.*;
import com.backend.nova.admin.entity.*;
import com.backend.nova.admin.repository.AdminRepository;
import com.backend.nova.apartment.entity.Apartment;
import com.backend.nova.auth.jwt.JwtProvider;
import com.backend.nova.auth.jwt.JwtToken;
import com.backend.nova.auth.otp.StatelessOtpService;
import com.backend.nova.global.exception.BusinessException;
import com.backend.nova.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminService {

    private final StatelessOtpService otpService;
    private final MailService mailService;
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;


    private static final int MAX_FAILED_ATTEMPTS = 5;

    /* ================= 관리자 생성 ================= */
    @Transactional
    public void createAdmin(AdminCreateRequest request, Long currentAdminId) {

        Admin currentAdmin = adminRepository.findById(currentAdminId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_NOT_FOUND));

        if (currentAdmin.getRole() != AdminRole.SUPER_ADMIN) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        if (!request.password().equals(request.passwordConfirm())) {
            throw new BusinessException(ErrorCode.PASSWORD_NOT_MATCH);
        }

        if (adminRepository.existsByLoginId(request.loginId())) {
            throw new BusinessException(ErrorCode.ADMIN_LOGIN_ID_DUPLICATED);
        }

        if (adminRepository.existsByEmail(request.email())) {
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
                .phoneNumber(request.phoneNumber())
                .birthDate(request.birthDate())
                .role(AdminRole.ADMIN)
                .status(AdminStatus.ACTIVE)
                .apartment(apartment)
                .build();

        adminRepository.save(admin);
    }

    /* ================= 로그인 ================= */
    public AdminMessageResponse login(AdminLoginRequest request) {

        Admin admin = adminRepository.findByLoginId(request.loginId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_LOGIN_FAILED));

        validateAdminStatus(admin);

        if (!passwordEncoder.matches(request.password(), admin.getPassword())) {
            handleLoginFailure(admin);
            throw new BusinessException(ErrorCode.ADMIN_LOGIN_FAILED);
        }

        handleLoginSuccess(admin);

        // OTP 발송
        String otp = otpService.generate(admin.getLoginId(), OtpPurpose.LOGIN);
        mailService.sendOtpMail(admin.getEmail(), otp);

        return new AdminMessageResponse("OTP가 발송되었습니다. 이메일을 확인하세요.");
    }

    /* ================= 로그인 OTP 검증 ================= */
    public AdminTokenResponse loginVerifyOtp(AdminLoginConfirmRequest request, HttpServletResponse response) {

        Admin admin = getAdminByLoginId(request.loginId());
        validateAdminStatus(admin);

        if (!otpService.verify(admin.getLoginId(), OtpPurpose.LOGIN, request.otpCode())) {
            throw new BusinessException(ErrorCode.OTP_INVALID);
        }

        // 토큰 생성
        JwtToken token = jwtProvider.generateAdminToken(admin);

        // Refresh Token을 HttpOnly 쿠키로 설정
        ResponseCookie cookie = ResponseCookie.from("refreshToken", token.refreshToken())
                .httpOnly(true)
                .secure(true) // HTTPS가 아니면 작동 안 할 수 있으니 로컬 개발시엔 false로 하거나 배포시 반드시 true
                .path("/")
                .maxAge(7 * 24 * 60 * 60) // 7일
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        // 클라이언트(프론트)에는 AccessToken과 관리자 정보만 전달
        return AdminTokenResponse.builder()
                .accessToken(token.accessToken())
                .adminId(admin.getId())
                .name(admin.getName())
                .role(admin.getRole().name())
                .build();
    }



    /* ================= 토큰 재발급 ================= */
    public AdminTokenResponse refresh(String refreshToken, HttpServletResponse response) {

        // 쿠키에서 온 토큰 검증 (레디스가 없으므로 블랙리스트 체크 제외하거나 DB 체크로 대체 가능)
        if (refreshToken == null || !jwtProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        Admin admin = getAdminByLoginId(jwtProvider.getSubject(refreshToken));
        validateAdminStatus(admin);

        // 새 토큰 생성
        JwtToken newToken = jwtProvider.generateAdminToken(admin);

        // 새 Refresh Token도 쿠키에 다시 구워줌 (Rotation 방식)
        ResponseCookie cookie = ResponseCookie.from("refreshToken", newToken.refreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(7 * 24 * 60 * 60)
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return AdminTokenResponse.builder()
                .accessToken(newToken.accessToken())
                .adminId(admin.getId())
                .name(admin.getName())
                .role(admin.getRole().name())
                .build();
    }

    /* ================= 로그아웃 ================= */
    public void logout(HttpServletResponse response) {
        // 쿠키의 유효기간을 0으로 설정하여 브라우저가 즉시 삭제하게 함
        // 유효기간(maxAge)을 0으로 설정한 쿠키를 응답 헤더에 실음
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0) // 즉시 삭제
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
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


    /* ================= 내 정보 조회 ================= */
    public AdminInfoResponse getAdminInfo(Long adminId) {
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_NOT_FOUND));

        Apartment apartment = admin.getApartment();

        return new AdminInfoResponse(
                admin.getId(),
                admin.getLoginId(),
                admin.getName(),
                admin.getEmail(),
                admin.getPhoneNumber(),
                admin.getBirthDate(),
                admin.getProfileImg(),
                admin.getRole() != null ? admin.getRole().name() : null,
                apartment != null ? apartment.getId() : null
        );
    }

    /* ================= 내 아파트 정보 조회 ================= */
    public AdminApartmentResponse getAdminApartmentInfo(Long adminId) {
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_NOT_FOUND));

        Apartment apartment = admin.getApartment();
        if (apartment == null) {
            throw new BusinessException(ErrorCode.APARTMENT_NOT_FOUND);
        }

        return new AdminApartmentResponse(
                apartment.getId(),
                apartment.getName(),
                apartment.getAddress()
        );
    }

    /* ================= 비밀번호 변경 요청(OTP 발송) ================= */
    public AdminMessageResponse requestChangePassword(
            AdminPasswordChangeRequest request,
            Long adminId
    ) {

        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_NOT_FOUND));

        validateAdminStatus(admin);

        // 현재 비밀번호 검증
        if (!passwordEncoder.matches(request.currentPassword(), admin.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_NOT_MATCH);
        }

        // OTP 발송
        String otp = otpService.generate(admin.getLoginId(), OtpPurpose.PASSWORD_CHANGE);
        mailService.sendOtpMail(admin.getEmail(), otp);

        return new AdminMessageResponse("OTP가 발송되었습니다.");
    }

    /* ================= 비밀번호 변경 확인 ================= */
    public AdminMessageResponse confirmChangePassword(
            AdminPasswordChangeConfirmRequest request,
            Long adminId
    ) {

        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_NOT_FOUND));

        validateAdminStatus(admin);

        // OTP 검증
        if (!otpService.verify(admin.getLoginId(), OtpPurpose.PASSWORD_CHANGE, request.otpCode())) {
            throw new BusinessException(ErrorCode.OTP_INVALID);
        }

        // 현재 비밀번호 검증
        if (!passwordEncoder.matches(request.currentPassword(), admin.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_NOT_MATCH);
        }

        // 새 비밀번호 검증
        if (!request.newPassword().equals(request.passwordConfirm())) {
            throw new BusinessException(ErrorCode.PASSWORD_CONFIRM_NOT_MATCH);
        }

        admin.setPassword(passwordEncoder.encode(request.newPassword()));

        return new AdminMessageResponse("비밀번호가 성공적으로 변경되었습니다.");
    }

    /* ================= 비밀번호 재설정 요청 ================= */
    public AdminMessageResponse requestResetPassword(AdminPasswordResetRequest request) {

        Admin admin = adminRepository
                .findByLoginIdAndEmail(request.loginId(), request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_NOT_FOUND));

        validateAdminStatus(admin);

        // OTP 발송
        String otp = otpService.generate(admin.getLoginId(), OtpPurpose.PASSWORD_RESET);
        mailService.sendOtpMail(admin.getEmail(), otp);

        return new AdminMessageResponse("비밀번호 재설정 OTP가 발송되었습니다.");
    }

    /* ================= 비밀번호 재설정 ================= */
    public AdminMessageResponse confirmResetPassword(AdminPasswordResetConfirmRequest request) {

        Admin admin = getAdminByLoginId(request.loginId());

        validateAdminStatus(admin);

        if (!otpService.verify(admin.getLoginId(), OtpPurpose.PASSWORD_RESET, request.otpCode())) {
            throw new BusinessException(ErrorCode.OTP_INVALID);
        }

        if (!request.newPassword().equals(request.passwordConfirm())) {
            throw new BusinessException(ErrorCode.PASSWORD_CONFIRM_NOT_MATCH);
        }

        admin.setPassword(passwordEncoder.encode(request.newPassword()));

        return new AdminMessageResponse("비밀번호가 성공적으로 변경되었습니다.");
    }

}
