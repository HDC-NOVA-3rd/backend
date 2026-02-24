package com.backend.nova.homeEnvironment.mode.scheduler;

import com.backend.nova.homeEnvironment.mode.entity.Mode;
import com.backend.nova.homeEnvironment.mode.entity.ModeSchedule;
import com.backend.nova.homeEnvironment.mode.repository.ModeRepository;
import com.backend.nova.homeEnvironment.mode.repository.ModeScheduleRepository;
import com.backend.nova.homeEnvironment.mode.service.ModeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ModeScheduleRunner {

    private final ModeScheduleRepository modeScheduleRepository;
    private final ModeRepository modeRepository;
    private final ModeService modeService;

    // 같은 분에 중복 실행 방지용 (scheduleId -> 실행 키)
    private final Map<Long, String> lastRun = new ConcurrentHashMap<>();

    // 매 분 0초에 예약 스케줄 체크
    @Scheduled(cron = "0 * * * * *")
    public void tick() {

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now().withSecond(0).withNano(0);
        DayOfWeek dow = today.getDayOfWeek();

        //log.info("[SCHEDULE] tick now={} dow={}", now, dow);

        var enabled = modeScheduleRepository.findAllByIsEnabledTrue();
        //log.info("[SCHEDULE] enabled schedules={}", enabled.size());

        for (ModeSchedule s : enabled) {

            // 오늘 요일이 반복 조건에 포함되는지 확인
            if (!matchDay(s.getRepeatDays(), dow)) continue;

            // 시작 시간 도달 시 해당 모드 실행
            if (s.getStartTime() != null) {
                LocalTime start = s.getStartTime().withSecond(0).withNano(0);

                if (now.equals(start)) {
                    String runKey = today + "_START_" + start;
                    String prev = lastRun.get(s.getId());
                    if (!runKey.equals(prev)) {
                        lastRun.put(s.getId(), runKey);

                        try {
                            modeService.executeModeByModeId(s.getMode().getId());
                            log.info("[SCHEDULE] START executed modeId={} scheduleId={}",
                                    s.getMode().getId(), s.getId());
                        } catch (Exception e) {
                            log.error("[SCHEDULE] START execute failed scheduleId={}", s.getId(), e);
                        }
                    }
                }
            }

            // 종료 시간 도달 시 전환 모드 실행, 없으면 귀가 기본 모드 실행
            if (s.getEndTime() != null) {
                LocalTime end = s.getEndTime().withSecond(0).withNano(0);

                if (now.equals(end)) {
                    String runKey = today + "_END_" + end;
                    String prev = lastRun.get(s.getId());
                    if (!runKey.equals(prev)) {
                        lastRun.put(s.getId(), runKey);

                        try {
                            if (s.getEndMode() != null) {
                                modeService.executeModeByModeId(s.getEndMode().getId());
                                log.info("[SCHEDULE] END-switch executed endModeId={} scheduleId={}",
                                        s.getEndMode().getId(), s.getId());
                            } else {
                                Long hoId = s.getMode().getHo().getId();

                                Optional<Mode> homeModeOpt =
                                        modeRepository.findFirstByHo_IdAndIsDefaultTrueAndModeNameOrderByIdAsc(hoId, "귀가");

                                if (homeModeOpt.isPresent()) {
                                    Mode homeMode = homeModeOpt.get();
                                    modeService.executeModeByModeId(homeMode.getId());
                                    log.info("[SCHEDULE] END-default executed homeModeId={} scheduleId={}",
                                            homeMode.getId(), s.getId());
                                } else {
                                    log.warn("[SCHEDULE] END reached but no default home mode found. hoId={} scheduleId={}",
                                            hoId, s.getId());
                                }
                            }
                        } catch (Exception e) {
                            log.error("[SCHEDULE] END execute failed scheduleId={}", s.getId(), e);
                        }
                    }
                }
            }
        }
    }

    private boolean matchDay(String repeatDays, DayOfWeek dow) {
        if (repeatDays == null || repeatDays.isBlank()) return false;

        String v = repeatDays.trim().toUpperCase();

        if (v.equals("DAILY")) return true;
        if (v.equals("WEEKDAY")) return dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY;

        String key = dow.name().substring(0, 3);
        for (String token : v.split(",")) {
            if (key.equals(token.trim())) return true;
        }
        return false;
    }
}