package com.backend.nova.chat.dto;

public record RoomDeviceExecuteReq(
        String traceId,
        String deviceCode,
        String command,
        Object value
) {}