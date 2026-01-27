package com.backend.nova.admin.dto;

import lombok.Getter;

@Getter
public class PasswordResetConfirmRequest {
    private String loginId;
    private String newPassword;
}
