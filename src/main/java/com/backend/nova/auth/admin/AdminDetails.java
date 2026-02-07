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
    private final String role;

    // 인증 상태 판단용 최소 필드
    private final AdminStatus status;
    private final LocalDateTime lockedUntil;

    public AdminDetails(Admin admin) {
        this.adminId = admin.getId();
        this.role = admin.getRole().name();
        this.status = admin.getStatus();
        this.lockedUntil = admin.getLockedUntil();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(
                new SimpleGrantedAuthority("ROLE_" + role)
        );
    }

    /**
     * JWT 인증에서는 username/password 거의 안 씀
     * null 반환해도 문제 없음
     */
    @Override
    public String getUsername() {
        return String.valueOf(adminId);
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return lockedUntil == null || lockedUntil.isBefore(LocalDateTime.now());
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == AdminStatus.ACTIVE;
    }
}
