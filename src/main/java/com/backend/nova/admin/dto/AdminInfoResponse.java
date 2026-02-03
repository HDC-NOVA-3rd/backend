package com.backend.nova.admin.dto;

import com.backend.nova.admin.entity.Admin;

import java.time.LocalDate;

public record AdminInfoResponse(
        String name,
        String email,
        String phoneNumber,
        LocalDate birthDate,
        String profileImg
) {
    public static AdminInfoResponse from(Admin admin) {
        return new AdminInfoResponse(
                admin.getName(),
                admin.getEmail(),
                admin.getPhoneNumber(),
                admin.getBirthDate(),
                admin.getProfileImg()
        );
    }
}
