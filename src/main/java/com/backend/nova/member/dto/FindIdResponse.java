package com.backend.nova.member.dto;

import com.backend.nova.member.entity.LoginType;
import lombok.Builder;

@Builder
public record FindIdResponse(
        LoginType loginType,
        String message
) {
}
