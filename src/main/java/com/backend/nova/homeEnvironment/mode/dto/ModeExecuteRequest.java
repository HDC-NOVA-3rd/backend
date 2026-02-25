package com.backend.nova.homeEnvironment.mode.dto;

// 지금 실행 요청 DTO
public record ModeExecuteRequest(
        boolean execute // 보통 true만 보냄
) {}
