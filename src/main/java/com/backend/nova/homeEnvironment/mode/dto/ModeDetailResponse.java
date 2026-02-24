package com.backend.nova.homeEnvironment.mode.dto;
import java.util.List;

// 모드 상세 화면 응답 DTO
public record ModeDetailResponse(
        Long modeId,
        String modeName,
        boolean isDefault,
        boolean isEditable,
        List<ActionItem> actions,
        List<ScheduleItem> schedules
) {
    // 상세 화면에 보여줄 액션 한 줄
    public record ActionItem(
            Integer sortOrder,
            Long deviceId,
            String deviceName, // 있으면 좋음 (없으면 null)
            String command,
            String value
    ) {}

    // 상세 화면에 보여줄 스케줄 한 줄
    public record ScheduleItem(
            String startTime,   // "23:00"
            String endTime,
            Long endModeId,
            String repeatDays,  // "MON,WED,FRI"
            boolean isEnabled
    ) {}
}
