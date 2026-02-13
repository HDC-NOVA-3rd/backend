package com.backend.nova.homeEnvironment.mode.dto;
import java.util.List;

// 예약 설정 요청 DTO
public record ModeScheduleSetRequest(
        List<ScheduleCreateItem> schedules
) {
    public record ScheduleCreateItem(
            String startTime,   // "23:00"
            String repeatDays,  // "MON,WED,FRI" or "DAILY"
            boolean isEnabled   // 토글값(보통 true)
    ) {}
}

