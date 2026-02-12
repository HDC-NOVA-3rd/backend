package com.backend.nova.admin.dto;


import java.time.LocalDate;

/* ================= 관리자 정보 응답 ================= */
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
