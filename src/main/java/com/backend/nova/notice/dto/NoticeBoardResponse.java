package com.backend.nova.notice.dto;

import com.backend.nova.notice.entity.Notice;

import java.time.LocalDateTime;

public record NoticeBoardResponse(
        Long noticeId,
        String title,
        String content,
        String authorName,
        LocalDateTime createdAt
) {
    public static NoticeBoardResponse from(Notice notice) {
        return new NoticeBoardResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getContent(),
                notice.getAdmin().getName(),
                notice.getCreatedAt()
        );
    }
}
