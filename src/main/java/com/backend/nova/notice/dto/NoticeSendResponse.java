package com.backend.nova.notice.dto;

public record NoticeSendResponse(
        boolean success,
        String message,
        int sentCount
) {
}
