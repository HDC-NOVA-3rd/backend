package com.backend.nova.chat.dto;

public record ExecuteCommandReq(
        String traceId,
        String command
) {}
