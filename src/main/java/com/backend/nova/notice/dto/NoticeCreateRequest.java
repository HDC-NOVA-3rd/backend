package com.backend.nova.notice.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record NoticeCreateRequest(
        @NotBlank(message = "title은 필수입니다.")
        String title,

        @NotBlank(message = "content는 필수입니다.")
        String content,

        List<Long> dongIds
) {
}
