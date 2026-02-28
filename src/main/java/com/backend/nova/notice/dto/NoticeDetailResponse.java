package com.backend.nova.notice.dto;

import com.backend.nova.notice.entity.Notice;
import com.backend.nova.notice.entity.NoticeTargetScope;

import java.time.LocalDateTime;
import java.util.List;

public record NoticeDetailResponse(
        Long noticeId,
        String title,
        String content,
        String authorName,
        NoticeTargetScope targetScope,
        List<Long> dongIds,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static NoticeDetailResponse from(Notice notice, List<Long> dongIds) {
        return new NoticeDetailResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getContent(),
                notice.getAdmin().getName(),
                notice.getTargetScope(),
                dongIds,
                notice.getCreatedAt(),
                notice.getUpdatedAt()
        );
    }
}
