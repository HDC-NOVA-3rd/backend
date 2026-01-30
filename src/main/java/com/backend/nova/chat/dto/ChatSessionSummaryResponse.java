package com.backend.nova.chat.dto;

public record ChatSessionSummaryResponse(
        String sessionId,
        String lastMessage,
        java.time.LocalDateTime lastMessageAt,
        String status
) {}
