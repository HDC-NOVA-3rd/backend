package com.backend.nova.homeEnvironment.dto;

import com.backend.nova.homeEnvironment.entity.Room;

public record RoomListItemResponse(
        Long roomId,
        String roomName,
        boolean isVisible
) {
    public static RoomListItemResponse from(Room r) {
        return new RoomListItemResponse(r.getId(), r.getName(), r.isVisible());
    }
}

