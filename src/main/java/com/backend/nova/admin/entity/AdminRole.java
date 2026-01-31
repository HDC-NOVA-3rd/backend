package com.backend.nova.admin.entity;

import org.springframework.security.core.GrantedAuthority;

public enum AdminRole implements GrantedAuthority {
    SUPER_ADMIN,     // 최고 관리자
    MANAGER,         // 배정 담당자
    ADMIN;            // 일반 관리자 (조회만)

    @Override
    public String getAuthority() {
        return "ROLE_" + name();
    }
}
