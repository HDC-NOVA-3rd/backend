package com.backend.nova.homeEnvironment.service;

import com.backend.nova.homeEnvironment.dto.DeviceStateUpdateRequest;
import com.backend.nova.homeEnvironment.entity.Device;
import com.backend.nova.homeEnvironment.entity.DeviceType;
import com.backend.nova.homeEnvironment.repository.DeviceRepository;
import com.backend.nova.homeEnvironment.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeviceStateService {

    private final RoomRepository roomRepository;
    private final DeviceRepository deviceRepository;

    // roomId 방에 있는 디바이스들의 상태를 부분 업데이트한다
    @Transactional
    public void patchDevicesState(Long roomId, DeviceStateUpdateRequest request) {

        roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("방을 찾을 수 없습니다. roomId=" + roomId));

        if (request == null || request.devices() == null || request.devices().isEmpty()) {
            throw new IllegalArgumentException("변경할 디바이스가 없습니다.");
        }

        for (DeviceStateUpdateRequest.DevicePatch patch : request.devices()) {
            if (patch == null || patch.deviceCode() == null || patch.deviceCode().isBlank()) {
                throw new IllegalArgumentException("deviceCode는 필수입니다.");
            }

            Device device = deviceRepository
                    .findByRoom_IdAndDeviceCode(roomId, patch.deviceCode())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "해당 방에 디바이스가 없습니다. roomId=" + roomId + ", deviceCode=" + patch.deviceCode()
                    ));

            // ===== 타입 검증 =====
            if (patch.brightness() != null && device.getType() != DeviceType.LED) {
                throw new IllegalArgumentException("밝기 조절은 LED만 가능합니다. deviceCode=" + device.getDeviceCode());
            }

            if (patch.targetTemp() != null && !(device.getType() == DeviceType.FAN || device.getType() == DeviceType.AIRCON)) {
                throw new IllegalArgumentException("온도 설정은 FAN/AIRCON만 가능합니다. deviceCode=" + device.getDeviceCode());
            }

            if (patch.autoMode() != null && !(device.getType() == DeviceType.FAN || device.getType() == DeviceType.AIRCON)) {
                throw new IllegalArgumentException("자동모드는 FAN/AIRCON만 가능합니다. deviceCode=" + device.getDeviceCode());
            }

            // ===== 업데이트 적용 =====
            boolean brightnessUpdated = false;

            if (patch.brightness() != null) {
                device.changeBrightness(patch.brightness());
                brightnessUpdated = true;
            }

            if (!brightnessUpdated && patch.power() != null) {
                device.changePower(patch.power());
            }

            if (patch.targetTemp() != null) {
                device.changeTargetTemp(patch.targetTemp());
            }

            if (patch.autoMode() != null) {
                device.changeAutoMode(patch.autoMode());
            }

            deviceRepository.save(device);
        }
    }
}