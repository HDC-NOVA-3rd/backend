package com.backend.nova.chat.dto;

public record ChatMessageResponse(
        String role,   // USER / ASSISTANT
        String content,
        java.time.LocalDateTime createdAt
) {}
