package com.backend.nova.homeEnvironment.mode.dto;

import com.backend.nova.homeEnvironment.mode.entity.ModeActionCommand;

import java.util.List;

public record ModeActionsUpsertRequest(
        List<ActionItem> actions
) {
    public record ActionItem(
            Integer  sortOrder,
            Long deviceId,
            ModeActionCommand command, // enum
            String value     // "ON"/"OFF"/"40"/"24"
    ) {}
}
