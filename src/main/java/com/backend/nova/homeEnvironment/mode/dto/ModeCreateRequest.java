package com.backend.nova.homeEnvironment.mode.dto;

// 커스텀 모드 생성(신규/복제) 요청 DTO
public record ModeCreateRequest(
        String modeName,     // 생성할 모드 이름
        Long sourceModeId    // 복제 생성이면 원본 modeId, 신규면 null
) {}

