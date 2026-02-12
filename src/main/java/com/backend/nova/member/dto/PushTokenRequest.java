package com.backend.nova.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record PushTokenRequest(
        @Schema(description = "Expo에서 발급받은 푸시 토큰", example = "ExponentPushToken[xxxxxxxxxxxxxxxxxxxxxx]")
        String pushToken
) {}
