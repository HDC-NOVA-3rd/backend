package com.backend.nova.admin.entity;

import com.backend.nova.apartment.entity.Apartment;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "admin",
        indexes = {
                @Index(name = "idx_admin_login_id", columnList = "login_id", unique = true),
                @Index(name = "idx_admin_email", columnList = "email", unique = true)
        }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Admin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 로그인 ID */
    @Column(name = "login_id", nullable = false, length = 50)
    private String loginId;

    /** 비밀번호 해시 */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    /** 관리자 이름 */
    @Column(nullable = false, length = 100)
    private String name;

    /** 이메일 (OTP 발송용) */
    @Column(nullable = false, length = 255)
    private String email;

    /** 계정 상태 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AdminStatus status;

    /** 관리자 권한 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AdminRole role;


    /** 관리자 폰 번호 */
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    /** 관리자 생년월일 */
    @Column(name = "birth_date")
    private LocalDate birthDate;

    /** 관리자 프로파일 */
    @Column(name = "profile_img", length = 500)
    private String profileImg;

    /** 단지 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "apartment_id", nullable = false)
    private Apartment apartment;


    /** 로그인 실패 횟수 */
    @Column(name = "failed_login_count", nullable = false, columnDefinition = "int default 0")
    private int failedLoginCount;

    /** 계정 잠금 해제 시각 */
    @Column(name = "locked_until", nullable = true)
    private LocalDateTime lockedUntil;

    /** 마지막 로그인 시각 */
    @Column(name = "last_login_at", nullable = true)
    private LocalDateTime lastLoginAt;

    /** 생성 시각 */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 수정 시각 */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /* ===== lifecycle only ===== */

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;

        if (this.status == null) {
            this.status = AdminStatus.ACTIVE;
        }
        if (this.role == null) {
            this.role = AdminRole.ADMIN;
        }
    }


    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }


}
