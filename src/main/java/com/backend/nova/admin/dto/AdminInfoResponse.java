package com.backend.nova.admin.dto;


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
) {}
