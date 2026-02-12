package com.backend.nova.homeEnvironment.dto;

import com.backend.nova.homeEnvironment.entity.Room;

public record RoomListItemResponse(
        Long roomId,
        String roomName
) {
    public static RoomListItemResponse from(Room room) {
        return new RoomListItemResponse(room.getId(), room.getName());
    }
}

