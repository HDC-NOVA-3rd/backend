package com.backend.nova.member.entity;

import com.backend.nova.resident.entity.Resident;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resident_id", nullable = false)
    private Resident resident;

    @Column(name = "login_id", nullable = false, unique = true)
    private String loginId;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false)
    private String name;

    @Column
    private String email;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "login_type", nullable = false)
    private LoginType loginType;

    @Column(name = "profile_img")
    private String profileImg;

    public void updateOAuthInfo(String provider, String providerId, String profileImage) {

        // 일반 가입자도 소셜 로그인을 허용하고, 타입 정보를 갱신한다.
        if (this.loginType == LoginType.NORMAL) {
            this.loginType = LoginType.valueOf(provider.toUpperCase());
            // 프로필 이미지 업데이트 로직 추가
            // 정책: 소셜 쪽 프로필 이미지가 존재하면 내 정보도 최신화한다.
            if (profileImage != null && !profileImage.isEmpty()) {
                this.profileImg = profileImage;
            }
        }
    }
}