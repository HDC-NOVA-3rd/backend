package com.backend.nova.auth.admin;


import com.backend.nova.admin.entity.Admin;
import com.backend.nova.admin.entity.AdminStatus;
import com.backend.nova.admin.entity.AdminRole;

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
    private final Long apartmentId;

    private final String loginId;
    private final String role;

    private final AdminStatus status;
    private final LocalDateTime lockedUntil;

    public AdminDetails(Admin admin) {
        this.adminId = admin.getId();
        this.loginId = admin.getLoginId();
        this.role = admin.getRole().name();
        this.status = admin.getStatus();
        this.lockedUntil = admin.getLockedUntil();
        this.apartmentId = admin.getApartment().getId(); // 여기서만 접근
    }

    public AdminRole getRoleEnum() {
        return AdminRole.valueOf(this.role);
    }

    public boolean isSuperAdmin() {
        return getRoleEnum() == AdminRole.SUPER_ADMIN;
    }


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(
                new SimpleGrantedAuthority("ROLE_" + role)
        );
    }

    @Override
    public String getUsername() {
        return loginId;
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
