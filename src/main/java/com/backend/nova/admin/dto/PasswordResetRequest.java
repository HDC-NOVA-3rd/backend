package com.backend.nova.admin.dto;

import lombok.Getter;

@Getter
public class PasswordResetRequest {
    private String loginId;
    private String email;
}
