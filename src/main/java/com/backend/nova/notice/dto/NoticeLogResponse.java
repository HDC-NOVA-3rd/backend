package com.backend.nova.notice.dto;

import com.backend.nova.notice.entity.NoticeSendLog;

import java.time.LocalDateTime;

public record NoticeLogResponse(
        Long id,
        String type,
        Long recipientId,
        String title,
        String content,
        LocalDateTime sentAt,
        boolean read
) {
    public static NoticeLogResponse from(NoticeSendLog log) {
        return new NoticeLogResponse(
                log.getId(),
                "notice",
                log.getRecipientId(),
                log.getTitle(),
                log.getContent(),
                log.getSentAt(),
                log.isRead()
        );
    }
}
