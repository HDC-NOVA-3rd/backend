package com.backend.nova.admin.dto;

import lombok.Getter;

@Getter
public class AdminLoginRequest {
    private String loginId;
    private String password;
}
