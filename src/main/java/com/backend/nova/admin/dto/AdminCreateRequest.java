package com.backend.nova.admin.dto;

import com.backend.nova.admin.entity.AdminRole;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AdminCreateRequest {

    private String loginId;
    private String password;
    private String name;
    private String email;
    private AdminRole role; // SUPER_ADMIN / ADMIN
}
