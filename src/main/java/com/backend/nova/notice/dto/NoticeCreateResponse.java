package com.backend.nova.notice.dto;

public record NoticeCreateResponse(
        boolean success,
        Long noticeId
) {
}
