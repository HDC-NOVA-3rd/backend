package com.backend.nova.admin.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

@Getter
public class AdminPrincipal implements UserDetails {

    private final Long adminId;
    private final String email;
    private final Collection<? extends GrantedAuthority> authorities;

    public AdminPrincipal(
            Long adminId,
            String email,
            Collection<? extends GrantedAuthority> authorities
    ) {
        this.adminId = adminId;
        this.email = email;
        this.authorities = authorities;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override public String getPassword() { return null; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
