package com.backend.nova.homeEnvironment.dto;

public record DeviceStateUpdateResponse(
        String result
) {
    public static DeviceStateUpdateResponse ok() {
        return new DeviceStateUpdateResponse("SUCCESS");
    }
}
