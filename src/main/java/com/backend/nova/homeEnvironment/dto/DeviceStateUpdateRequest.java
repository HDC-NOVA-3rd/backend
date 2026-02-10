package com.backend.nova.homeEnvironment.dto;

import java.util.List;

public record DeviceStateUpdateRequest(
        List<DevicePatch> devices
) {
    public record DevicePatch(
            String deviceCode,     // 필수
            Boolean power,         // 선택
            Integer brightness,    // 선택 (LED)
            Integer targetTemp     // 선택 (AIRCON)
    ) {}
}
