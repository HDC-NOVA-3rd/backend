package com.backend.nova.homeEnvironment.mode.service;

import com.backend.nova.apartment.entity.Ho;
import com.backend.nova.apartment.repository.HoRepository;
import com.backend.nova.homeEnvironment.mode.dto.ModeDetailResponse;
import com.backend.nova.homeEnvironment.mode.dto.ModeListItemResponse;
import com.backend.nova.homeEnvironment.mode.entity.Mode;
import com.backend.nova.homeEnvironment.mode.entity.ModeAction;
import com.backend.nova.homeEnvironment.mode.entity.ModeSchedule;
import com.backend.nova.homeEnvironment.mode.repository.ModeActionRepository;
import com.backend.nova.homeEnvironment.mode.repository.ModeRepository;
import com.backend.nova.homeEnvironment.mode.repository.ModeScheduleRepository;
import com.backend.nova.member.entity.Member;
import com.backend.nova.member.repository.MemberRepository;
import com.backend.nova.mqtt.MqttAssistantPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.backend.nova.homeEnvironment.entity.Device;
import com.backend.nova.homeEnvironment.entity.DeviceType;
import com.backend.nova.homeEnvironment.repository.DeviceRepository;
import com.backend.nova.homeEnvironment.mode.entity.ModeActionCommand;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ModeService {
    private final MqttAssistantPublisher mqttAssistantPublisher;
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
            if (modeActionRepository.existsByMode_Id(m.getId())) continue; // 이미 액션 있으면 스킵

            switch (m.getModeName()) {
                case "외출" -> seedOutingActions(m, devices);
                case "취침" -> seedSleepActions(m, devices);
                case "귀가" -> seedHomeActions(m, devices);
            }
        }
    }

    private void seedOutingActions(Mode outing, List<Device> devices) {
        int order = 1;
        for (Device d : devices) {
            modeActionRepository.save(ModeAction.builder()
                    .mode(outing)
                    .sortOrder(order++)
                    .device(d)
                    .command(ModeActionCommand.POWER)
                    .value("OFF")
                    .build());
        }
    }

    private void seedSleepActions(Mode sleep, List<Device> devices) {
        // "침/안" 방 우선
        List<Device> bedRoomDevices = devices.stream()
                .filter(d -> d.getRoom() != null && d.getRoom().getName() != null)
                .filter(d -> d.getRoom().getName().contains("침") || d.getRoom().getName().contains("안"))
                .toList();

        Device sleepLed = bedRoomDevices.stream().filter(d -> d.getType() == DeviceType.LED).findFirst()
                .orElseGet(() -> devices.stream().filter(d -> d.getType() == DeviceType.LED).findFirst().orElse(null));

        Device sleepAircon = bedRoomDevices.stream().filter(d -> d.getType() == DeviceType.AIRCON).findFirst()
                .orElseGet(() -> devices.stream().filter(d -> d.getType() == DeviceType.AIRCON).findFirst().orElse(null));

        Device sleepFan = bedRoomDevices.stream().filter(d -> d.getType() == DeviceType.FAN).findFirst()
                .orElseGet(() -> devices.stream().filter(d -> d.getType() == DeviceType.FAN).findFirst().orElse(null));

        int order = 1;

        // 수면등(밝기 10), 에어컨 26도 ON, 팬 ON (없으면 skip)
        if (sleepLed != null) {
            modeActionRepository.save(ModeAction.builder()
                    .mode(sleep)
                    .sortOrder(order++)
                    .device(sleepLed)
                    .command(ModeActionCommand.BRIGHTNESS)
                    .value("10")
                    .build());
        }
        if (sleepAircon != null) {
            modeActionRepository.save(ModeAction.builder()
                    .mode(sleep)
                    .sortOrder(order++)
                    .device(sleepAircon)
                    .command(ModeActionCommand.SET_TEMP)
                    .value("26")
                    .build());
            modeActionRepository.save(ModeAction.builder()
                    .mode(sleep)
                    .sortOrder(order++)
                    .device(sleepAircon)
                    .command(ModeActionCommand.POWER)
                    .value("ON")
                    .build());
        }
        if (sleepFan != null) {
            modeActionRepository.save(ModeAction.builder()
                    .mode(sleep)
                    .sortOrder(order++)
                    .device(sleepFan)
                    .command(ModeActionCommand.POWER)
                    .value("ON")
                    .build());
        }
    }

    private void seedHomeActions(Mode home, List<Device> devices) {
        Device homeAircon = devices.stream().filter(d -> d.getType() == DeviceType.AIRCON).findFirst().orElse(null);
        Device homeFan = devices.stream().filter(d -> d.getType() == DeviceType.FAN).findFirst().orElse(null);

        int order = 1;

        // 귀가 = 쾌적온도 24~25 맞춰놓기 + 팬 ON
        if (homeAircon != null) {
            modeActionRepository.save(ModeAction.builder()
                    .mode(home)
                    .sortOrder(order++)
                    .device(homeAircon)
                    .command(ModeActionCommand.SET_TEMP)
                    .value("24")
                    .build());
            modeActionRepository.save(ModeAction.builder()
                    .mode(home)
                    .sortOrder(order++)
                    .device(homeAircon)
                    .command(ModeActionCommand.POWER)
                    .value("ON")
                    .build());
        }
        if (homeFan != null) {
            modeActionRepository.save(ModeAction.builder()
                    .mode(home)
                    .sortOrder(order++)
                    .device(homeFan)
                    .command(ModeActionCommand.POWER)
                    .value("ON")
                    .build());
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
        Long hoId = getHoIdByLoginId(loginId);

        Mode mode = getMyModeDetail(loginId, modeId);
        List<ModeAction> actions = modeActionRepository.findAllByMode_IdOrderBySortOrderAsc(mode.getId());

        if (actions.isEmpty()) throw new IllegalStateException("모드에 등록된 액션이 없습니다.");

        for (ModeAction a : actions) {
            Device d = a.getDevice();
            String v = a.getValue();

            switch (a.getCommand()) {
                case POWER -> {
                    boolean on = "ON".equalsIgnoreCase(v) || "TRUE".equalsIgnoreCase(v);
                    d.changePower(on);
                    mqttAssistantPublisher.publishCommand(hoId, d.getDeviceCode(), "POWER", on);
                }
                case BRIGHTNESS -> {
                    int b = Integer.parseInt(v);
                    d.changeBrightness(b);
                    mqttAssistantPublisher.publishCommand(hoId, d.getDeviceCode(), "BRIGHTNESS", b);
                }
                case SET_TEMP -> {
                    int t = Integer.parseInt(v);
                    d.changeTargetTemp(t);
                    d.changePower(true);
                    mqttAssistantPublisher.publishCommand(hoId, d.getDeviceCode(), "SET_TEMP", t);
                    mqttAssistantPublisher.publishCommand(hoId, d.getDeviceCode(), "POWER", true);
                }
            }
        }
    }
    // MQTT 붙일 거면 여기서 actions대로 publish 하면 됨


}
