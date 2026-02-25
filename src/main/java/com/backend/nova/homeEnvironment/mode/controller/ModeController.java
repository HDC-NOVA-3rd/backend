package com.backend.nova.homeEnvironment.mode.controller;

import com.backend.nova.homeEnvironment.mode.dto.*;
import com.backend.nova.homeEnvironment.mode.entity.Mode;
import com.backend.nova.homeEnvironment.mode.entity.ModeSchedule;
import com.backend.nova.homeEnvironment.mode.service.ModeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Tag(
        name = "Mode",
        description = "자동화 모드(기본/커스텀) 조회, 생성, 실행, 예약, 숨김/삭제 API"
)
@SecurityRequirement(name = "bearerAuth") // Swagger에 'Authorize'로 토큰 필요 표시 (설정이 있을 때만 의미 있음)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mode")
public class ModeController {

    private final ModeService modeService;

    @Operation(
            summary = "내 모드 목록 조회",
            description = """
                    로그인한 사용자의 세대(ho)에 속한 모드 목록을 조회합니다.
                    - 숨김 처리된 모드는 제외합니다.
                    - 각 모드 카드에 표시할 예약 요약(scheduleSummary)을 함께 내려줍니다.
                    """
    )
    @GetMapping("/my")
    public List<ModeListItemResponse> getMyModes(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User user
    ) {
        return modeService.getMyModes(user.getUsername());
    }

    @Operation(
            summary = "모드 상세 조회",
            description = """
                    특정 모드의 상세 정보를 조회합니다.
                    - 모드에 등록된 실행 동작(actions) 목록
                    - 모드에 등록된 예약 정보(schedules) 목록
                    ※ 다른 세대의 modeId는 조회할 수 없도록 서버에서 소유권을 검증합니다.
                    """
    )
    @GetMapping("/{modeId}")
    public ModeDetailResponse getMyModeDetail(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User user,
            @PathVariable Long modeId
    ) {
        return modeService.getMyModeDetailResponse(user.getUsername(), modeId);
    }

    @Operation(
            summary = "커스텀 모드 생성(신규/복제)",
            description = """
                    커스텀 모드를 생성합니다.
                    - 신규 생성: sourceModeId를 null로 전송
                    - 복제 생성: sourceModeId에 복제할 원본 modeId 전송(보통 기본 모드)
                    ※ 같은 세대 내 modeName 중복은 허용하지 않습니다.
                    """
    )
    @PostMapping("/my")
    public ModeCreateResponse createMyMode(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User user,
            @RequestBody ModeCreateRequest request
    ) {
        Mode created = modeService.createMyCustomMode(user.getUsername(), request.modeName(), request.sourceModeId());
        return new ModeCreateResponse(created.getId(), created.getModeName(), "CREATED");
    }

    @Operation(
            summary = "모드 즉시 실행",
            description = """
                    선택한 모드를 즉시 실행합니다.
                    - 등록된 actions를 sortOrder 순서대로 실행하는 것을 전제로 합니다.
                    - 현재는 실행 로직(MQTT publish 등)을 TODO로 두고 API 연결부터 완성하는 단계입니다.
                    """
    )
    @PatchMapping("/{modeId}/execute")
    public Map<String, Object> executeMyMode(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User user,
            @Parameter(description = "실행할 모드 ID", example = "1")
            @PathVariable Long modeId
    ) {
        modeService.executeMyMode(user.getUsername(), modeId);
        return Map.of("modeId", modeId, "status", "EXECUTED");
    }

    @Operation(
            summary = "모드 예약 설정(교체)",
            description = """
                    모드 예약 정보를 설정합니다.
                    - 요청으로 받은 schedules로 기존 예약을 삭제 후 재등록(교체)합니다.
                    - 프론트의 예약 토글/시간/요일 설정을 저장하는 용도입니다.
                    """
    )
    @PatchMapping("/{modeId}/schedule")
    public Map<String, Object> setMyModeSchedule(
            @AuthenticationPrincipal User user,
            @PathVariable Long modeId,
            @RequestBody ModeScheduleSetRequest request
    ) {
        modeService.setMyModeSchedulesFromDto(user.getUsername(), modeId, request);
        return Map.of("modeId", modeId, "status", "SCHEDULED");
    }

    @Operation(
            summary = "모드 예약 해제(전체 삭제)",
            description = """
                    해당 모드에 설정된 예약 정보를 전부 해제합니다.
                    - 내부적으로 modeId에 연결된 mode_schedule을 모두 삭제합니다.
                    - 예약 토글 OFF 또는 '예약 해제' 버튼에 대응합니다.
                    """
    )
    @DeleteMapping("/{modeId}/schedule")
    public Map<String, Object> clearMyModeSchedule(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User user,
            @Parameter(description = "예약을 해제할 모드 ID", example = "1")
            @PathVariable Long modeId
    ) {
        modeService.clearMyModeSchedules(user.getUsername(), modeId);
        return Map.of("modeId", modeId, "status", "CLEARED");
    }

    @Operation(
            summary = "모드 숨김/표시 변경",
            description = """
                    모드를 목록에서 숨기거나 다시 표시합니다.
                    - 기본 모드도 삭제 대신 '숨김'으로 처리할 수 있습니다.
                    - visible=false면 목록에서 제외됩니다.
                    """
    )
    @PatchMapping("/{modeId}/visibility")
    public Map<String, Object> updateVisibility(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User user,
            @Parameter(description = "숨김/표시를 변경할 모드 ID", example = "1")
            @PathVariable Long modeId,
            @RequestBody ModeVisibilityRequest request
    ) {
        modeService.updateMyModeVisibility(user.getUsername(), modeId, request.visible());
        return Map.of("modeId", modeId, "status", "UPDATED");
    }

    @Operation(
            summary = "모드 삭제(커스텀만)",
            description = """
                    커스텀 모드를 삭제합니다.
                    - 기본 모드는 삭제 불가(삭제 대신 숨김 사용)
                    - 삭제 시 actions/schedules는 연관관계(cascade)로 함께 정리됩니다.
                    """
    )
    @DeleteMapping("/{modeId}")
    public Map<String, Object> deleteMyMode(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User user,
            @Parameter(description = "삭제할 모드 ID", example = "10")
            @PathVariable Long modeId
    ) {
        modeService.deleteMyMode(user.getUsername(), modeId);
        return Map.of("result", "DELETED");
    }

    // 액션 "전체 교체" 저장
    @Operation(
            summary = "모드 실행 동작(actions) 전체 교체 저장",
            description = """
                특정 모드에 등록된 실행 동작(actions)을 '전체 교체' 방식으로 저장합니다.
                - 기존 actions는 모두 삭제 후, 요청으로 받은 actions로 다시 저장합니다.
                - 커스텀 모드(편집 가능)만 수정할 수 있습니다. (기본 모드/편집불가 모드는 거부)
                """
    )
    @PutMapping("/{modeId}/action")
    public void setModeActions(
            @AuthenticationPrincipal(expression = "username") String loginId,
            @PathVariable Long modeId,
            @RequestBody ModeActionsUpsertRequest request
    ) {
        modeService.setMyModeActions(loginId, modeId, request);
    }

    @Operation(
            summary = "내 모드 목록 조회(숨김 포함)",
            description = "설정 화면용. 숨김 처리된 모드도 포함해서 조회합니다."
    )
    @GetMapping("/my/all")
    public List<ModeListItemResponse> getMyModesAll(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User user
    ) {
        return modeService.getMyModesAll(user.getUsername());
    }
}
