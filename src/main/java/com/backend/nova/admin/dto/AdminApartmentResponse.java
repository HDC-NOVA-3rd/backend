package com.backend.nova.admin.dto;


/* ================= 관리자 아파트 정보 응답 ================= */
public record AdminApartmentResponse(
        Long apartmentId,
        String apartmentName,
        String address
) {}
