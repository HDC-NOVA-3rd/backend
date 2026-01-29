package com.backend.nova.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminLoginRequest(

        @NotBlank(message = "loginId는 필수입니다.")
        String loginId,

        @NotBlank(message = "password는 필수입니다.")
        String password
) {}
