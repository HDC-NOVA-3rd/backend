package com.backend.nova.homeEnvironment.mode.dto;

// 목록
public record ModeListItemResponse(
        Long modeId,
        String modeName,
        boolean isDefault,
        boolean isEditable,
        boolean isVisible,
        boolean isScheduled,       // 예약이 있는지(요약용)
        String scheduleSummary     // "DAILY 23:00" 같은 문자열(없으면 null)
) {}
