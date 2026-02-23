package com.backend.nova.resident.dto;

public record ResidentCreateRequest(
        Long hoId,
        String name,
        String phone,
        String dong,  // 프론트의 formData.dong과 일치해야 함
        String ho     // 프론트의 formData.ho와 일치해야 함
) {
}