package com.backend.nova.admin.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "admin_mfa_otp",
        indexes = {
                @Index(name = "idx_admin_purpose", columnList = "admin_id,purpose"),
                @Index(name = "idx_expires_at", columnList = "expires_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AdminMfaOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 어떤 관리자 OTP인지 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private Admin admin;

    /** OTP 코드 */
    @Column(name = "otp_code", nullable = false, length = 10)
    private String otpCode;

    /** OTP 용도 (LOGIN / PASSWORD_RESET) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OtpPurpose purpose;

    /** 만료 시간 */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** 시도 횟수 */
    @Column(name = "attempt_count", nullable = false)
    @Builder.Default
    private int attemptCount = 0;


    /** 검증 완료 시각 (null이면 미검증) */
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    /** 생성 시각 */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /* ========= lifecycle ========= */

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    /* ========= domain logic ========= */

    public void increaseAttempt() {
        this.attemptCount++;
    }

    public void markVerified() {
        this.verifiedAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return expiresAt.isBefore(LocalDateTime.now());
    }

    public boolean isVerified() {
        return verifiedAt != null;
    }
}