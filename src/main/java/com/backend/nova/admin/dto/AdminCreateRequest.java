package com.backend.nova.admin.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record AdminCreateRequest(

        @NotBlank(message = "이름은 필수입니다.")
        @Size(max = 100)
        String name,

        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        @Size(max = 255)
        String email,

        @NotBlank(message = "로그인 ID는 필수입니다.")
        @Size(min = 4, max = 50, message = "로그인 ID는 4~50자입니다.")
        String loginId,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, max = 100, message = "비밀번호는 최소 8자 이상입니다.")
        String password,

        @NotBlank String passwordConfirm,

        @Pattern(regexp = "^[0-9\\-]{9,20}$", message = "전화번호 형식이 올바르지 않습니다.")
        String phoneNumber,

        LocalDate birthDate
) {}