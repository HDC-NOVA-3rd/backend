package com.backend.nova.homeEnvironment.controller;

import com.backend.nova.homeEnvironment.dto.DeviceStateUpdateRequest;
import com.backend.nova.homeEnvironment.dto.DeviceStateUpdateResponse;
import com.backend.nova.homeEnvironment.dto.RoomSnapshotResponse;
import com.backend.nova.homeEnvironment.service.DeviceStateService;
import com.backend.nova.homeEnvironment.service.RoomSnapshotService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/room")
public class RoomController {

    private final RoomSnapshotService roomSnapshotService;
    private final DeviceStateService deviceStateService;
    // 방 상세 진입 시 스냅샷(최신 온습도 + 디바이스 현재상태) 조회
    @GetMapping("/{roomId}/snapshot")
    public RoomSnapshotResponse getRoomSnapshot(@PathVariable Long roomId) {
        return roomSnapshotService.getSnapshot(roomId);
    }
    // 방의 디바이스 상태를 일괄 업데이트 (전등/팬/목표온도 등)
    @PatchMapping("/{roomId}/devices/state")
    public DeviceStateUpdateResponse patchRoomDevicesState(
            @PathVariable Long roomId,
            @RequestBody DeviceStateUpdateRequest request
    ) {
        deviceStateService.patchDevicesState(roomId, request);
        return DeviceStateUpdateResponse.ok();
    }
}
