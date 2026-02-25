package com.backend.nova.homeEnvironment.mode.dto;

//모드 숨김/표시 변경 요청 DTO
public record ModeVisibilityRequest(
        boolean visible
) {}
