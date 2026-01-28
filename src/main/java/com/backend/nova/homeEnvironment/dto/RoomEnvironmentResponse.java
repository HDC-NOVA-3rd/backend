package com.backend.nova.homeEnvironment.dto;

public record RoomEnvironmentResponse(
        Long roomId,
        String roomName,
        Integer temperature,
        Integer humidity
) {}
