package com.backend.nova.homeEnvironment.mode.dto;

// 생성 결과 응답 DTO
public record ModeCreateResponse(
        Long modeId,
        String modeName,
        String result // "CREATED"
) {}
