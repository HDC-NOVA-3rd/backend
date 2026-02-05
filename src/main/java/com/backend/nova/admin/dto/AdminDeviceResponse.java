package com.backend.nova.admin.dto;

import java.time.LocalDateTime;

public record AdminDeviceResponse(
        Long id,
        String deviceId,
        boolean trusted,
        String lastIp,
        LocalDateTime lastUsedAt,
        LocalDateTime createdAt
) {}
