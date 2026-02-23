package com.backend.nova.admin.entity;

import org.springframework.security.core.GrantedAuthority;

public enum AdminRole implements GrantedAuthority {
    SUPER_ADMIN,     // 단지 최고 관리자
    ADMIN;           // 단지 일반 관리자

    @Override
    public String getAuthority() {
        return "ROLE_" + name();
    }
}
