package com.backend.nova.chat.dto;


public record ExecuteCommandRes(
        String traceId,
        String result
) {}