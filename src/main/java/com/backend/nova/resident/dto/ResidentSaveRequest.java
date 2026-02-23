package com.backend.nova.resident.dto;

public record ResidentSaveRequest(
        Long hoId,
        String name,
        String phone,
        String dong,
        String ho
) {
}