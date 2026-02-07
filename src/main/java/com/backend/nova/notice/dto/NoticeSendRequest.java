package com.backend.nova.notice.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record NoticeSendRequest(
        List<@NotNull Long> dongIds
) {
}
