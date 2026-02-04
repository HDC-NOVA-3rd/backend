package com.backend.nova.auth.admin;

import com.backend.nova.admin.entity.Admin;
import com.backend.nova.admin.entity.AdminStatus;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;

@Getter
public class AdminDetails implements UserDetails {

    private final Long adminId;
    private final String loginId;          // 로그인 아이디
    private final String password;         // 비밀번호 해시
    private final String name;             // 관리자 이름
    private final Long apartmentId;        // 관리자가 속한 단지 ID
    private final AdminStatus status;      // 계정 상태 (ACTIVE, INACTIVE 등)
    private final LocalDateTime lockedUntil; // 계정 잠금 해제 시각
    private final String role;             // 관리자 권한 (ADMIN, SUPER_ADMIN 등)

    public AdminDetails(Admin admin) {
        this.adminId = admin.getId();
        this.loginId = admin.getLoginId();
        this.password = admin.getPassword();
        this.name = admin.getName();
        this.apartmentId = admin.getApartment().getId(); // 단지 정보
        this.status = admin.getStatus();
        this.lockedUntil = admin.getLockedUntil();
        this.role = admin.getRole().name();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // ROLE_ADMIN, ROLE_SUPER_ADMIN 형태로 권한 부여
        return Collections.singleton(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getUsername() {
        // Spring Security에서 로그인 아이디 반환
        return this.loginId;
    }

    @Override
    public String getPassword() {
        // Spring Security에서 비밀번호 반환
        return this.password;
    }

    @Override
    public boolean isAccountNonExpired() {
        // 계정 만료 정책이 없으면 항상 true
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        // lockedUntil가 null이거나 과거면 계정 잠금 해제
        return lockedUntil == null || lockedUntil.isBefore(LocalDateTime.now());
    }

    @Override
    public boolean isCredentialsNonExpired() {
        // 비밀번호 만료 정책 없으면 항상 true
        return true;
    }

    @Override
    public boolean isEnabled() {
        // 상태가 ACTIVE여야 활성화
        return status == AdminStatus.ACTIVE;
    }
}
