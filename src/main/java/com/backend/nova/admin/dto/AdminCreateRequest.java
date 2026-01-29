package com.backend.nova.admin.dto;

import com.backend.nova.admin.entity.AdminRole;

public record AdminCreateRequest(
        String loginId,
        String password,
        String name,
        String email,
        AdminRole role
) {
    public AdminCreateRequest {
        if (loginId == null || loginId.isBlank()) {
            throw new IllegalArgumentException("loginId는 필수입니다.");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("password는 필수입니다.");
        }
    }
}
