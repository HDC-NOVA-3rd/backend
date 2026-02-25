package com.backend.nova.homeEnvironment.dto;

import com.backend.nova.homeEnvironment.entity.Device;
import com.backend.nova.homeEnvironment.entity.DeviceType;

public record DeviceSnapshot(
        Long deviceId,
        String deviceCode,
        String name,
        DeviceType type,
        Boolean power,
        Integer brightness,
        Integer targetTemp,
        Boolean autoMode
) {
    public static DeviceSnapshot from(Device d) {
        return new DeviceSnapshot(
                d.getId(),
                d.getDeviceCode(),
                d.getName(),
                d.getType(),
                d.getPower(),
                d.getBrightness(),
                d.getTargetTemp(),
                d.getAutoMode()
        );
    }
}