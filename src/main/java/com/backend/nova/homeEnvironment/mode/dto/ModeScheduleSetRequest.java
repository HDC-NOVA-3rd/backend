package com.backend.nova.homeEnvironment.mode.dto;
import java.util.List;

// 예약 설정 요청 DTO
// ModeScheduleSetRequest
public record ModeScheduleSetRequest(
        List<ScheduleCreateItem> schedules
) {
    public record ScheduleCreateItem(
            String startTime,      // "23:00"
            String endTime,        // 추가: "23:30" or null
            Long endModeId,        // 추가: 종료 시 전환할 modeId or null
            String repeatDays,     // "MON,WED,FRI" or "DAILY"
            boolean isEnabled
    ) {}
}

