package com.backend.nova.homeEnvironment.mode.service;

import com.backend.nova.apartment.entity.Ho;
import com.backend.nova.apartment.repository.HoRepository;
import com.backend.nova.homeEnvironment.mode.dto.ModeDetailResponse;
import com.backend.nova.homeEnvironment.mode.dto.ModeListItemResponse;
import com.backend.nova.homeEnvironment.mode.dto.ModeScheduleSetRequest;
import com.backend.nova.homeEnvironment.mode.entity.Mode;
import com.backend.nova.homeEnvironment.mode.entity.ModeAction;
import com.backend.nova.homeEnvironment.mode.entity.ModeSchedule;
import com.backend.nova.homeEnvironment.mode.repository.ModeActionRepository;
import com.backend.nova.homeEnvironment.mode.repository.ModeRepository;
import com.backend.nova.homeEnvironment.mode.repository.ModeScheduleRepository;
import com.backend.nova.member.entity.Member;
import com.backend.nova.member.repository.MemberRepository;
import com.backend.nova.mqtt.MqttCommandPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.backend.nova.homeEnvironment.entity.Device;
import com.backend.nova.homeEnvironment.entity.DeviceType;
import com.backend.nova.homeEnvironment.repository.DeviceRepository;
import com.backend.nova.homeEnvironment.mode.entity.ModeActionCommand;

import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import com.backend.nova.homeEnvironment.mode.dto.ModeActionsUpsertRequest;
import java.util.ArrayList;
import java.util.Objects;
@Slf4j
@Service
@RequiredArgsConstructor
public class ModeService {
    private final MqttCommandPublisher mqttCommandPublisher;
    private final MemberRepository memberRepository;
    private final HoRepository hoRepository; // 추가
    private final ModeRepository modeRepository;
    private final ModeActionRepository modeActionRepository;
    private final ModeScheduleRepository modeScheduleRepository;
    // 모드 액션 추가
    private final DeviceRepository deviceRepository;
    // 로그인 아이디(loginId)로 "내 hoId"를 찾기 위한 공통 로직
    private Long getHoIdByLoginId(String loginId) {
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("회원 없음: " + loginId));

        if (member.getResident() == null) {
            throw new IllegalStateException("거주지(resident) 정보가 없습니다.");
        }
        return member.getResident().getHo().getId();
    }

    @Transactional
    public List<ModeListItemResponse> getMyModes(String loginId) {

        Long hoId = getHoIdByLoginId(loginId);
        // 기본 모드가 없으면 최초 1회 자동 생성
        DefaultModes(hoId);
        // 모드 엔티티만 조회(연관 스케줄에 직접 접근하지 않음)
        List<Mode> modes = modeRepository.findAllByHo_IdAndIsVisibleTrueOrderByModeNameAsc(hoId);

        // scheduleSummary는 modeScheduleRepository로 대표 스케줄 1개만 따로 조회해서 만든다
        return modes.stream().map(m -> {
            var s = modeScheduleRepository.findFirstByMode_IdOrderByIdAsc(m.getId());

            boolean isScheduled = (s != null);
            String summary = null;
            if (isScheduled && s.getRepeatDays() != null && s.getStartTime() != null) {
                summary = s.getRepeatDays() + " " + s.getStartTime();
            }

            return new ModeListItemResponse(
                    m.getId(),
                    m.getModeName(),
                    m.isDefault(),
                    m.isEditable(),
                    m.isVisible(),
                    isScheduled,
                    summary
            );
        }).toList();
    }


    @Transactional(readOnly = true)
    public Mode getMyModeDetail(String loginId, Long modeId) {
        // 로그인 사용자의 hoId 소유인지 검증하고 모드 상세를 조회하기 위함
        Long hoId = getHoIdByLoginId(loginId);
        return modeRepository.findByIdAndHo_Id(modeId, hoId)
                .orElseThrow(() -> new IllegalArgumentException("모드를 찾을 수 없습니다. modeId=" + modeId));
    }

    @Transactional
    public Mode createMyCustomMode(String loginId, String modeName, Long sourceModeId) {
        // 로그인 사용자의 hoId를 구해서 "내 세대에 속한 모드"로 생성하기 위함
        Long hoId = getHoIdByLoginId(loginId);

        // hoId로 Ho 엔티티를 조회해 Mode에 정확히 연결하기 위함
        Ho ho = hoRepository.findById(hoId)
                .orElseThrow(() -> new IllegalArgumentException("세대를 찾을 수 없습니다. hoId=" + hoId));

        // 같은 세대에서 이름 중복을 막기 위함
        if (modeRepository.existsByHo_IdAndModeName(hoId, modeName)) {
            throw new IllegalArgumentException("이미 존재하는 모드 이름입니다. modeName=" + modeName);
        }

        // 복제 생성: 내 세대(hoId)의 원본 모드를 찾아 액션을 복사하기 위함
        if (sourceModeId != null) {
            Mode source = modeRepository.findByIdAndHo_Id(sourceModeId, hoId)
                    .orElseThrow(() -> new IllegalArgumentException("복제할 원본 모드를 찾을 수 없습니다. sourceModeId=" + sourceModeId));

            Mode copied = Mode.builder()
                    .ho(ho)
                    .modeName(modeName)
                    .isDefault(false)
                    .isVisible(true)
                    .isEditable(true)
                    .build();

            modeRepository.save(copied);

            List<ModeAction> sourceActions = modeActionRepository.findAllByMode_IdOrderBySortOrderAsc(source.getId());
            for (ModeAction a : sourceActions) {
                modeActionRepository.save(ModeAction.builder()
                        .mode(copied)
                        .sortOrder(a.getSortOrder())
                        .device(a.getDevice())
                        .command(a.getCommand())
                        .value(a.getValue())
                        .build());
            }

            // 정책: 복제 시 스케줄은 복제하지 않음(원본 유지/사고 방지)
            return copied;
        }

        // 신규 생성: 액션 없는 빈 커스텀 모드를 만들고 이후 편집에서 채우기 위함
        return modeRepository.save(Mode.builder()
                .ho(ho)
                .modeName(modeName)
                .isDefault(false)
                .isVisible(true)
                .isEditable(true)
                .build());
    }

    @Transactional
    public void DefaultModes(Long hoId) {
        Ho ho = hoRepository.findById(hoId)
                .orElseThrow(() -> new IllegalArgumentException("세대를 찾을 수 없습니다. hoId=" + hoId));

        // 세대 전체 디바이스 (액션 만들 때 필요)
        List<Device> devices = deviceRepository.findAllByRoom_Ho_Id(hoId);

        // 1) 기본 모드가 없으면 생성
        List<Mode> defaults = modeRepository.findAllByHo_IdAndIsDefaultTrue(hoId);
        if (defaults.isEmpty()) {
            Mode outing = modeRepository.save(Mode.builder()
                    .ho(ho).modeName("외출")
                    .isDefault(true).isEditable(false).isVisible(true)
                    .build());

            Mode sleep = modeRepository.save(Mode.builder()
                    .ho(ho).modeName("취침")
                    .isDefault(true).isEditable(false).isVisible(true)
                    .build());

            Mode home = modeRepository.save(Mode.builder()
                    .ho(ho).modeName("귀가")
                    .isDefault(true).isEditable(false).isVisible(true)
                    .build());

            defaults = List.of(outing, sleep, home);
        }

        // 디바이스 없으면 모드만 있고 액션은 못 만듦
        if (devices.isEmpty()) return;

        // 2)기본모드가 "이미 있어도", 액션이 비어있으면 채워넣기
        for (Mode m : defaults) {
            // 기존 액션 삭제 후 항상 재시드
            if (modeActionRepository.existsByMode_Id(m.getId())) continue;

            switch (m.getModeName()) {
                case "외출" -> seedOutingActions(m, devices);
                case "취침" -> seedSleepActions(m, devices);
                case "귀가" -> seedHomeActions(m, devices);
            }
        }
    }

    private void seedOutingActions(Mode outing, List<Device> devices) {
        seedAllOff(outing, devices);
    }

    private void seedSleepActions(Mode sleep, List<Device> devices) {
        seedAllOff(sleep, devices);
    }

    private void seedAllOff(Mode mode, List<Device> devices) {
        int order = 1;

        for (Device d : devices) {
            modeActionRepository.save(ModeAction.builder()
                    .mode(mode)
                    .sortOrder(order++)
                    .device(d)
                    .command(ModeActionCommand.POWER)
                    .value("OFF")
                    .build());
        }
    }

    private void seedHomeActions(Mode home, List<Device> devices) {
        int order = 1;

        for (Device d : devices) {
            if (d.getType() == DeviceType.LED) {
                modeActionRepository.save(ModeAction.builder()
                        .mode(home)
                        .sortOrder(order++)
                        .device(d)
                        .command(ModeActionCommand.POWER)
                        .value("ON")
                        .build());
            } else {
                modeActionRepository.save(ModeAction.builder()
                        .mode(home)
                        .sortOrder(order++)
                        .device(d)
                        .command(ModeActionCommand.POWER)
                        .value("OFF")
                        .build());
            }
        }
    }



    @Transactional
    public void setMyModeSchedules(String loginId, Long modeId, List<ModeSchedule> schedulesToCreate) {
        // 로그인 사용자의 모드인지 검증한 뒤 예약을 교체(삭제 후 재등록)하기 위함
        Mode mode = getMyModeDetail(loginId, modeId);

        modeScheduleRepository.deleteAllByMode_Id(mode.getId());

        for (ModeSchedule s : schedulesToCreate) {
            modeScheduleRepository.save(ModeSchedule.builder()
                    .mode(mode)
                    .startTime(s.getStartTime())
                    .endTime(s.getEndTime())
                    .endMode(s.getEndMode())
                    .repeatDays(s.getRepeatDays())
                    .isEnabled(s.isEnabled())
                    .build());
        }
    }

    @Transactional
    public void clearMyModeSchedules(String loginId, Long modeId) {
        // 로그인 사용자의 모드인지 검증한 뒤 예약을 완전히 해제(삭제)하기 위함
        Mode mode = getMyModeDetail(loginId, modeId);
        modeScheduleRepository.deleteAllByMode_Id(mode.getId());
    }

    @Transactional
    public void updateMyModeVisibility(String loginId, Long modeId, boolean visible) {
        // 로그인 사용자의 모드인지 검증한 뒤 숨김/표시를 변경하기 위함
        Mode mode = getMyModeDetail(loginId, modeId);
        if (visible) mode.show();
        else mode.hide();
    }

    @Transactional
    public void deleteMyMode(String loginId, Long modeId) {
        // 로그인 사용자의 커스텀 모드만 삭제하도록 검증하고 삭제하기 위함
        Mode mode = getMyModeDetail(loginId, modeId);

        if (mode.isDefault()) {
            throw new IllegalStateException("기본 모드는 삭제할 수 없습니다.");
        }
        modeRepository.delete(mode);
    }

    @Transactional(readOnly = true)
    public ModeDetailResponse getMyModeDetailResponse(String loginId, Long modeId) {
        Mode mode = getMyModeDetail(loginId, modeId);

        var actions = modeActionRepository.findAllByMode_IdOrderBySortOrderAsc(mode.getId()).stream()
                .map(a -> new ModeDetailResponse.ActionItem(
                        a.getSortOrder(),
                        a.getDevice().getId(),
                        a.getDevice().getName(),
                        a.getCommand().name(),
                        a.getValue()
                ))
                .toList();

        var schedules = modeScheduleRepository.findAllByMode_Id(mode.getId()).stream()
                .map(s -> new ModeDetailResponse.ScheduleItem(
                        s.getStartTime() != null ? s.getStartTime().toString() : null,
                        s.getEndTime() != null ? s.getEndTime().toString() : null,
                        s.getEndMode() != null ? s.getEndMode().getId() : null,
                        s.getRepeatDays(),
                        s.isEnabled()
                ))
                .toList();

        return new ModeDetailResponse(
                mode.getId(),
                mode.getModeName(),
                mode.isDefault(),
                mode.isEditable(),
                actions,
                schedules
        );
    }
    @Transactional
    public void executeMyMode(String loginId, Long modeId) {
        // 소유권 검증
        getMyModeDetail(loginId, modeId);

        // 실제 실행은 공통 메서드
        executeModeByModeId(modeId);
    }
    @Transactional
    public void executeModeByModeId(Long modeId) {
        Mode mode = modeRepository.findById(modeId)
                .orElseThrow(() -> new IllegalArgumentException("모드를 찾을 수 없습니다. modeId=" + modeId));

        Long hoId = mode.getHo().getId();

        List<ModeAction> actions = modeActionRepository.findAllByMode_IdOrderBySortOrderAsc(modeId);
        if (actions.isEmpty()) throw new IllegalStateException("모드에 등록된 액션이 없습니다.");

        for (ModeAction a : actions) {
            Device d = a.getDevice();
            String v = a.getValue();

            if (d == null || d.getRoom() == null) {
                throw new IllegalStateException("액션에 디바이스/룸이 연결되지 않았습니다. actionId=" + a.getId());
            }

            Long roomId = d.getRoom().getId();

            switch (a.getCommand()) {
                case POWER -> {
                    boolean on = "ON".equalsIgnoreCase(v) || "TRUE".equalsIgnoreCase(v);

                    // DB 반영
                    d.changePower(on);

                    // ✅ MQTT는 roomId 포함해서 device-req로 발행
                    mqttCommandPublisher.publishDeviceCommand(hoId, roomId, d.getDeviceCode(), "POWER", on);

                    // LED 켜질 때 밝기 기본값 보정
                    if (d.getType() == DeviceType.LED && on) {
                        Integer cur = d.getBrightness();
                        if (cur == null || cur <= 0) {
                            int defaultB = 50;
                            d.changeBrightness(defaultB);
                            mqttCommandPublisher.publishDeviceCommand(hoId, roomId, d.getDeviceCode(), "BRIGHTNESS", defaultB);
                        }
                    }

                    // LED 꺼질 때 밝기 0 동기화
                    if (d.getType() == DeviceType.LED && !on) {
                        d.changeBrightness(0);
                        mqttCommandPublisher.publishDeviceCommand(hoId, roomId, d.getDeviceCode(), "BRIGHTNESS", 0);
                    }
                }

                case BRIGHTNESS -> {
                    int b = Integer.parseInt(v);

                    d.changeBrightness(b);

                    boolean on = b > 0;
                    d.changePower(on);

                    // ✅ 순서 중요: 밝기 -> 전원 or 전원 -> 밝기 는 Pi 구현에 맞춰
                    // 보통은 POWER 먼저 주고 BRIGHTNESS 주는 게 안전
                    mqttCommandPublisher.publishDeviceCommand(hoId, roomId, d.getDeviceCode(), "POWER", on);
                    mqttCommandPublisher.publishDeviceCommand(hoId, roomId, d.getDeviceCode(), "BRIGHTNESS", b);
                }

                case SET_TEMP -> {
                    int t = Integer.parseInt(v);

                    d.changeTargetTemp(t);
                    d.changePower(true);

                    mqttCommandPublisher.publishDeviceCommand(hoId, roomId, d.getDeviceCode(), "SET_TEMP", t);
                    mqttCommandPublisher.publishDeviceCommand(hoId, roomId, d.getDeviceCode(), "POWER", true);
                }
            }
        }
    }
    // MQTT 붙일 거면 여기서 actions대로 publish 하면 됨
    @Transactional
    public void setMyModeActions(String loginId, Long modeId, ModeActionsUpsertRequest req) {
        if (req == null || req.actions() == null) {
            throw new IllegalArgumentException("actions가 비어있습니다.");
        }

        Mode mode = getMyModeDetail(loginId, modeId);

        if (!mode.isEditable() || mode.isDefault()) {
            throw new IllegalStateException("기본 모드/편집 불가 모드는 동작을 수정할 수 없습니다.");
        }

        Long hoId = getHoIdByLoginId(loginId);

        // 1) 기존 액션 전체 삭제
        modeActionRepository.deleteAllByMode_Id(mode.getId());
        modeActionRepository.flush(); // ✅ 중요: delete를 DB에 먼저 반영

        // actions 비어있으면 “전체 삭제”로 처리하고 종료
        if (req.actions().isEmpty()) return;

        // ✅ 2) 중복 정리: (deviceId + command) 기준으로 마지막 것만 남김
        List<ModeActionsUpsertRequest.ActionItem> normalized = normalizeActionsKeepLast(req.actions());

        // ✅ 3) 정규화된 목록으로 저장 (sortOrder는 서버가 1..N 재부여)
        int sortOrder = 1;

        for (var a : normalized) {
            if (a.deviceId() == null) throw new IllegalArgumentException("deviceId는 필수입니다.");
            if (a.command() == null) throw new IllegalArgumentException("command는 필수입니다.");
            if (a.value() == null) throw new IllegalArgumentException("value는 필수입니다.");

            Device device = deviceRepository.findById(a.deviceId())
                    .orElseThrow(() -> new IllegalArgumentException("디바이스 없음: deviceId=" + a.deviceId()));

            // 내 세대(hoId) 디바이스인지 검증
            if (device.getRoom() == null || device.getRoom().getHo() == null ||
                    !device.getRoom().getHo().getId().equals(hoId)) {
                throw new IllegalStateException("내 세대 디바이스만 등록 가능합니다. deviceId=" + a.deviceId());
            }

            ModeActionCommand cmd = a.command();

            // value 검증/정규화
            String value = a.value().trim();
            switch (cmd) {
                case POWER -> {
                    String upper = value.toUpperCase();
                    if (!(upper.equals("ON") || upper.equals("OFF") || upper.equals("TRUE") || upper.equals("FALSE"))) {
                        throw new IllegalArgumentException("POWER 값은 ON/OFF(TRUE/FALSE)만 허용됩니다.");
                    }
                    value = (upper.equals("TRUE")) ? "ON" : (upper.equals("FALSE") ? "OFF" : upper);
                }
                case BRIGHTNESS -> {
                    int b;
                    try { b = Integer.parseInt(value); }
                    catch (Exception e) { throw new IllegalArgumentException("BRIGHTNESS는 숫자여야 합니다."); }
                    if (b < 0 || b > 100) throw new IllegalArgumentException("BRIGHTNESS 범위는 0~100 입니다.");
                    value = String.valueOf(b);
                }
                case SET_TEMP -> {
                    int t;
                    try { t = Integer.parseInt(value); }
                    catch (Exception e) { throw new IllegalArgumentException("SET_TEMP는 숫자여야 합니다."); }
                    if (t < 16 || t > 30) throw new IllegalArgumentException("SET_TEMP 범위는 16~30 입니다.");
                    value = String.valueOf(t);
                }
            }

            modeActionRepository.save(ModeAction.builder()
                    .mode(mode)
                    .sortOrder(sortOrder++) // ✅ 서버에서 1..N로 재부여
                    .device(device)
                    .command(cmd)
                    .value(value)
                    .build());
        }
    }

    /**
     * (deviceId + command) 기준으로 마지막 것만 남기되,
     * "마지막 등장 순서"대로 리스트를 반환한다.
     */
    private List<ModeActionsUpsertRequest.ActionItem> normalizeActionsKeepLast(
            List<ModeActionsUpsertRequest.ActionItem> actions
    ) {
        // LinkedHashMap: 삽입 순서 유지
        LinkedHashMap<ActionKey, ModeActionsUpsertRequest.ActionItem> map = new LinkedHashMap<>();

        for (var a : actions) {
            if (a == null) continue;
            if (a.deviceId() == null || a.command() == null) continue;

            ActionKey key = new ActionKey(a.deviceId(), a.command());

            // ✅ “마지막 등장” 순서를 만들기 위해: 기존이 있으면 제거 후 다시 put(맨 뒤로 이동)
            if (map.containsKey(key)) {
                map.remove(key);
            }
            map.put(key, a);
        }

        return new ArrayList<>(map.values());
    }

    // (deviceId, command) 키
    private static final class ActionKey {
        private final Long deviceId;
        private final ModeActionCommand command;

        private ActionKey(Long deviceId, ModeActionCommand command) {
            this.deviceId = deviceId;
            this.command = command;
        }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ActionKey other)) return false;
            return Objects.equals(deviceId, other.deviceId) && command == other.command;
        }

        @Override public int hashCode() {
            return Objects.hash(deviceId, command);
        }
    }

    @Transactional
    public List<ModeListItemResponse> getMyModesAll(String loginId) {
        Long hoId = getHoIdByLoginId(loginId);
        DefaultModes(hoId);

        List<Mode> modes = modeRepository.findAllByHo_IdOrderByModeNameAsc(hoId);

        return modes.stream().map(m -> {
            var s = modeScheduleRepository.findFirstByMode_IdOrderByIdAsc(m.getId());

            boolean isScheduled = (s != null);
            String summary = null;
            if (isScheduled && s.getRepeatDays() != null && s.getStartTime() != null) {
                summary = s.getRepeatDays() + " " + s.getStartTime();
            }

            return new ModeListItemResponse(
                    m.getId(),
                    m.getModeName(),
                    m.isDefault(),
                    m.isEditable(),
                    m.isVisible(),
                    isScheduled,
                    summary
            );
        }).toList();
    }
    @Transactional
    public void setMyModeSchedulesFromDto(String loginId, Long modeId, ModeScheduleSetRequest request) {
        Mode mode = getMyModeDetail(loginId, modeId); // ✅ 내 모드 소유권 검증

        modeScheduleRepository.deleteAllByMode_Id(mode.getId());

        Long hoId = getHoIdByLoginId(loginId);

        for (var s : request.schedules()) {
            Mode endMode = null;

            if (s.endModeId() != null) {
                // endMode도 내 세대 모드인지 검증
                endMode = modeRepository.findByIdAndHo_Id(s.endModeId(), hoId)
                        .orElseThrow(() -> new IllegalArgumentException("endModeId가 유효하지 않습니다. endModeId=" + s.endModeId()));
            }

            modeScheduleRepository.save(ModeSchedule.builder()
                    .mode(mode)
                    .startTime(s.startTime() != null ? LocalTime.parse(s.startTime()) : null)
                    .endTime(s.endTime() != null ? LocalTime.parse(s.endTime()) : null)
                    .endMode(endMode)
                    .repeatDays(s.repeatDays())
                    .isEnabled(s.isEnabled())
                    .build());
        }
    }
}
