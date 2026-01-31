package com.backend.nova.admin.dto;

import com.backend.nova.admin.entity.AdminRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminCreateRequest(

        @NotBlank(message = "loginId는 필수입니다.")
        @Size(max = 50)
        String loginId,

        @NotBlank(message = "password는 필수입니다.")
        @Size(min = 8, message = "password는 최소 8자 이상이어야 합니다.")
        String password,

        @NotBlank(message = "name은 필수입니다.")
        String name,

        @NotBlank(message = "email은 필수입니다.")
        @Email(message = "email 형식이 올바르지 않습니다.")
        String email,

        AdminRole role,

        @NotNull(message = "apartmentId는 필수입니다.")
        Long apartmentId
) {}
