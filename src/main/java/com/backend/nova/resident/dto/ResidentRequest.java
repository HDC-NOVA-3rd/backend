package com.backend.nova.resident.dto;

public record ResidentRequest(
        Long hoId,
        String name,
        String phone
) {
}