package com.backend.nova.resident.dto;

public record ResidentRequestDto(
        Long hoId,
        String name,
        String phone
) {
}