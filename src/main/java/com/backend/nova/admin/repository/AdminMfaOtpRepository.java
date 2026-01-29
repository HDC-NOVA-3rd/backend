package com.backend.nova.admin.repository;

import com.backend.nova.admin.entity.Admin;
import com.backend.nova.admin.entity.AdminMfaOtp;
import com.backend.nova.admin.entity.OtpPurpose;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AdminMfaOtpRepository extends JpaRepository<AdminMfaOtp, Long> {

    /** 가장 최근 미검증 OTP */
    Optional<AdminMfaOtp>
    findTopByAdminAndPurposeAndVerifiedAtIsNullOrderByCreatedAtDesc(
            Admin admin,
            OtpPurpose purpose
    );

    /** OTP 검증 완료 여부 확인 (비밀번호 재설정용) */
    boolean existsByAdminAndPurposeAndVerifiedAtIsNotNull(
            Admin admin,
            OtpPurpose purpose
    );

    /** 만료 OTP 정리 */
    void deleteByExpiresAtBefore(LocalDateTime now);
}
