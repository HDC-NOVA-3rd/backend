package com.backend.nova.homeEnvironment.controller;

import com.backend.nova.homeEnvironment.dto.*;
import com.backend.nova.homeEnvironment.service.DeviceStateService;
import com.backend.nova.homeEnvironment.service.RoomCommandService;
import com.backend.nova.homeEnvironment.service.RoomQueryService;
import com.backend.nova.homeEnvironment.service.RoomSnapshotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(
        name = "Room",
        description = "방 조회 및 방 내 디바이스 상태 제어 API"
)

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/room")
public class RoomController {
    private final RoomQueryService roomQueryService;
    private final RoomSnapshotService roomSnapshotService;
    private final DeviceStateService deviceStateService;
    private final RoomCommandService roomCommandService;
    // 방 상세 진입 시 스냅샷(최신 온습도 + 디바이스 현재상태) 조회
    @Operation(
            summary = "방 스냅샷 조회",
            description = "방 상세 화면 진입 시 필요한 초기 데이터(최신 온습도 + 디바이스 현재 상태)를 조회합니다."
    )
    @GetMapping("/{roomId}/snapshot")
    public RoomSnapshotResponse getRoomSnapshot(@PathVariable Long roomId) {
        return roomSnapshotService.getSnapshot(roomId);
    }
    // 방의 디바이스 상태를 일괄 업데이트 (전등/팬/목표온도 등)
    @Operation(
            summary = "방 디바이스 상태 변경",
            description = "특정 방에 포함된 디바이스들의 상태(전원, 밝기, 목표 온도 등)를 부분 업데이트합니다."
    )
    @PatchMapping("/{roomId}/devices/state")
    public DeviceStateUpdateResponse patchRoomDevicesState(
            @PathVariable Long roomId,
            @RequestBody DeviceStateUpdateRequest request
    ) {
        deviceStateService.patchDevicesState(roomId, request);
        return DeviceStateUpdateResponse.ok();
    }
    // 세대(hoId) 기준 방 목록 조회
    @Operation(
            summary = "세대 기준 방 목록 조회",
            description = "특정 세대(hoId)에 속한 모든 방 목록을 조회합니다."
    )
    @GetMapping("/ho/{hoId}")
    public List<RoomListItemResponse> getRoomsByHo(@PathVariable Long hoId) {
        return roomQueryService.getRoomsByHo(hoId);
    }
    @Operation(
            summary = "내 방 목록 조회",
            description = "로그인한 사용자의 계정 정보 기준으로 접근 가능한 방 목록을 조회합니다."
    )
    @GetMapping("/my")
    public List<RoomListItemResponse> getMyRooms(@AuthenticationPrincipal org.springframework.security.core.userdetails.User user) {
        return roomQueryService.getRoomsByLoginId(user.getUsername());
    }
    @Operation(
            summary = "방 숨김/표시 변경",
            description = "홈 화면에서 방을 숨기거나 다시 표시합니다."
    )
    @PatchMapping("/{roomId}/visibility")
    public Map<String, Object> updateRoomVisibility(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User user,
            @PathVariable Long roomId,
            @RequestBody RoomVisibilityRequest request
    ) {
        roomCommandService.updateRoomVisibility(user.getUsername(), roomId, request.visible());
        return Map.of("roomId", roomId, "status", "UPDATED", "visible", request.visible());
    }
}
