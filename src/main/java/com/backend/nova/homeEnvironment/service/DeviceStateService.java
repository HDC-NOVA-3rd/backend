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

    /**
     * roomId 방에 있는 디바이스들의 상태를 부분 업데이트한다.
     * - request.devices 안에 들어온 값만 반영
     */
    @Transactional
    public void patchDevicesState(Long roomId, DeviceStateUpdateRequest request) {

        // 1) 방 존재 체크 (없는 roomId면 바로 에러)
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

            // 1) 밝기 먼저 (밝기가 오면 power는 brightness가 결정)
            boolean brightnessUpdated = false;
            if (patch.brightness() != null) {
                if (device.getType() != DeviceType.LED) {
                    throw new IllegalArgumentException("밝기 조절은 LED만 가능합니다. deviceCode=" + device.getDeviceCode());
                }
                device.changeBrightness(patch.brightness());
                brightnessUpdated = true;
            }

            // 2) power는 brightness가 같이 안 온 경우에만 적용
            if (!brightnessUpdated && patch.power() != null) {
                device.changePower(patch.power());
            }

            // 3) 나머지
            if (patch.targetTemp() != null) device.changeTargetTemp(patch.targetTemp());
            deviceRepository.save(device);
        }
    }
}
