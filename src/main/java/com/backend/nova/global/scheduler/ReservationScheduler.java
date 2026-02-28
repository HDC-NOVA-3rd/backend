package com.backend.nova.global.scheduler;

import com.backend.nova.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationScheduler {

    private final ReservationService reservationService;

    // 정각 기준 2분 단위로 스케쥴러 실행 (2, 4, 6, 8 ...58, 0)
    @Scheduled(cron = "0 0/2 * * * *")
    public void scheduleReservationUpdates() {
        log.info("2분 단위 -> 예약 관리 스케줄러 실행");

        // 1. 입장 시간 된 예약 -> IN_USE 변경 & 알림
        reservationService.activateUpcomingReservations();

        // 2. 종료 임박 예약 -> 알림 발송
        reservationService.notifyEndingSoonReservations();

        // 3. 종료된 예약 -> COMPLETED 변경 & QR 만료
        reservationService.expireFinishedReservations();
    }
}
