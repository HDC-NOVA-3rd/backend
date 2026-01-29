package com.backend.nova.admin.entity;

import org.springframework.security.core.GrantedAuthority;

public enum AdminRole implements GrantedAuthority {
    ADMIN, SUPER_ADMIN;

    @Override
    public String getAuthority() {
        return "ROLE_" + name();
    }
}
