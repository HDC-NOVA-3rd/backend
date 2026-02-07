package com.backend.nova.homeEnvironment.dto;

import java.util.List;

public record RoomSnapshotResponse(
        Long roomId,
        String roomName,
        Integer temperature,
        Integer humidity,
        List<DeviceSnapshot> device
) {}
