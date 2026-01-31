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
    private final String loginId;
    private final String password;
    private final Long apartmentId; // 관리자가 속한 단지 ID

    private final AdminStatus status;
    private final LocalDateTime lockedUntil;

    public AdminDetails(Admin admin) {
        this.adminId = admin.getId();
        this.loginId = admin.getLoginId();
        this.password = admin.getPassword(); // 엔티티 필드명에 맞게 수정
        this.apartmentId = admin.getApartment().getId(); // 단지 정보 가져오기
        this.status = admin.getStatus();
        this.lockedUntil = admin.getLockedUntil();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // ROLE_ADMIN 권한 부여
        return Collections.singleton(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.loginId;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // 만료 정책 없으면 true
    }

    @Override
    public boolean isAccountNonLocked() {
        return lockedUntil == null || lockedUntil.isBefore(LocalDateTime.now());
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // 비밀번호 만료 정책 없으면 true
    }

    @Override
    public boolean isEnabled() {
        return this.status == AdminStatus.ACTIVE;
    }
}