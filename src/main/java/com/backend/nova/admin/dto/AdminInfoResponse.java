package com.backend.nova.admin.dto;

import com.backend.nova.admin.entity.Admin;

import java.time.LocalDate;

public record AdminInfoResponse(
        Long id,
        String loginId,
        String name,
        String email,
        String phoneNumber,
        LocalDate birthDate,
        String profileImg,
        String role,
        Long apartmentId
) {
    public static AdminInfoResponse from(Admin admin) {
        return null;
    }
}