package com.backend.nova.homeEnvironment.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
public class RoomEnvironmentResponse {
    private Long roomId;
    private Double temperature;
    private Integer humidity;

    @Builder
    public RoomEnvironmentResponse(Long roomId, Double temperature, Integer humidity) {
        this.roomId = roomId;
        this.temperature = temperature;
        this.humidity = humidity;
    }
}
