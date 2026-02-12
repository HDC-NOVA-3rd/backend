package com.backend.nova.reservation.service;

import com.backend.nova.facility.entity.Space;
import com.backend.nova.facility.repository.SpaceRepository;
import com.backend.nova.global.notification.NotificationService;
import com.backend.nova.global.notification.PushMessageRequest;
import com.backend.nova.member.entity.Member;
import com.backend.nova.member.repository.MemberRepository;
import com.backend.nova.reservation.dto.OccupiedReservationResponse;
import com.backend.nova.reservation.dto.ReservationRequest;
import com.backend.nova.reservation.dto.ReservationResponse;
import com.backend.nova.reservation.entity.Reservation;
import com.backend.nova.reservation.entity.Status;
import com.backend.nova.reservation.repository.ReservationRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final SpaceRepository spaceRepository;
    private final MemberRepository memberRepository; // 회원 조회용
    private final NotificationService notificationService;

    /**
     * 내 예약 목록 조회
     */
    public List<ReservationResponse> getMyReservations(Long memberId) {
        return reservationRepository.findAllByMemberIdOrderByStartTimeDesc(memberId)
                .stream()
                .map(ReservationResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 예약 상세 조회
     */
    public ReservationResponse getReservationDetails(Long memberId, Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예약입니다."));

        if (!reservation.getMember().getId().equals(memberId)) {
            throw new IllegalArgumentException("해당 예약에 대한 권한이 없습니다.");
        }

        return ReservationResponse.from(reservation);
    }

    /**
     * 예약 취소
     */
    @Transactional
    public void cancelReservation(Long memberId, Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예약입니다."));

        if (!reservation.getMember().getId().equals(memberId)) {
            throw new IllegalArgumentException("해당 예약에 대한 권한이 없습니다.");
        }

        reservation.cancel();
    }

    /**
     * 날짜와 공간 기반 이미 예약된 목록 조회
     */
    public List<OccupiedReservationResponse> getOccupiedReservations(Long spaceId, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        return reservationRepository.findAllBySpaceIdAndDate(spaceId, startOfDay, endOfDay)
                .stream()
                .map(OccupiedReservationResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 예약 생성 메서드
     */
    @Transactional
    public Long createReservation(Long memberId, ReservationRequest request) {

        // 1. 공간(Space) 조회 및 검증
        Space space = spaceRepository.findById(request.spaceId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공간입니다."));

        // 2. 인원 수(Capacity) 검증
        if (request.capacity() < space.getMinCapacity() || request.capacity() > space.getMaxCapacity()) {
            throw new IllegalArgumentException("이용 가능 인원을 벗어났습니다.");
        }

        // 3. 시간 유효성 검사 (종료 시간이 시작 시간보다 앞서는지 등)
        if (request.startTime().isAfter(request.endTime()) || request.startTime().isEqual(request.endTime())) {
            throw new IllegalArgumentException("잘못된 시간 설정입니다.");
        }

        // 4. [핵심] 중복 예약 체크 (DB 조회)
        // 트랜잭션 내에서 실행되므로, 이 시점에 겹치는 예약이 있으면 예외를 발생시킵니다.
        boolean isOverlapped = reservationRepository.existsOverlappingReservation(
                space.getId(),
                request.startTime(),
                request.endTime()
        );

        if (isOverlapped) {
            throw new IllegalStateException("해당 시간에 이미 예약이 존재합니다.");
        }

        // 5. 가격 계산 (30분 단위 계산) -> 프런트에서도 30분 단위로 선택할 수 있도록
        long minutes = Duration.between(request.startTime(), request.endTime()).toMinutes();
        // 최소 예약 시간 제한 (예: 최소 30분)
        if (minutes < 30) {
            throw new IllegalArgumentException("최소 예약 시간은 30분입니다.");
        }
        // 가격 계산: (분 / 60.0) * 시간당 가격
        double hours = minutes / 60.0;
        int totalPrice = (int) (hours * space.getPrice());

        Member member = memberRepository.getReferenceById(memberId);

        // 6. 예약 엔티티 생성
        Reservation reservation = Reservation.builder()
                .space(space)
                .member(member)
                .startTime(request.startTime())
                .endTime(request.endTime())
                .capacity(request.capacity())
                .totalPrice(totalPrice)
                .ownerName(request.ownerName())
                .ownerPhone(request.ownerPhone())
                .paymentMethod(request.paymentMethod())
                .qrToken(UUID.randomUUID().toString()) // 입장용 QR 토큰 생성
                .status(Status.CONFIRMED)   // 혹은 결제 전이면 PENDING
                .build();

        // 7. 저장
        Reservation savedReservation = reservationRepository.save(reservation);

        return savedReservation.getId();
    }

    /**
     * [스케줄러용] 시작 10분 전 예약 활성화 (CONFIRMED -> INUSE)
     */
    @Transactional
    public void activateUpcomingReservations() {
        LocalDateTime startTime = LocalDateTime.now().plusMinutes(10);

        // 1. 조건에 맞는 예약 조회 (상태: CONFIRMED, 시간: 예약시작시간 <= 현재시간+10분)
        List<Reservation> targets = reservationRepository.findAllByStatusAndStartTimeBefore(Status.CONFIRMED, startTime);

        // 1. 전송할 DTO 리스트 생성
        List<PushMessageRequest> messages = new ArrayList<>();

        for (Reservation reservation : targets) {
            // 상태 변경 CONFIRMED -> INUSE
            reservation.changeStatus(Status.INUSE);

            Member member = reservation.getMember();
            String pushToken = member.getPushToken();

            // 토큰이 유효한 경우만 메시지 생성
            if (pushToken != null && !pushToken.isBlank()) {

                // 깔끔하게 DTO 생성 (Builder 패턴 활용)
                PushMessageRequest message = PushMessageRequest.builder()
                        .to(pushToken)
                        .title("입장 안내")
                        .body("예약하신 [" + reservation.getSpace().getName() + "] 이 현재 입장 가능합니다.")
                        .data(Map.of("url", "/member/reservations"))  // expo에서 push될 route
                        .build();

                messages.add(message);
            }
        }

        // 2. 알림 서비스에 전송 위임 (배치 전송)
        if (!messages.isEmpty()) {
            notificationService.sendPushMessages(messages);
        }
    }

    /**
     * [스케줄러용] 종료 10분 전 알림 (INUSE 상태인 예약 중 종료 시간이 10분 전인 사람 해당)
     */
    @Transactional
    public void notifyEndingSoonReservations() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endTime = now.plusMinutes(10);

        // 1. 조건에 맞는 예약 조회 (상태: INUSE, 시간: endTime이 '현재' ~ '10분 뒤' 사이인 예약)
        // 종료 시간이 정확히 10분 남은 예약 조회 (범위 검색 추천)
        List<Reservation> targets = reservationRepository.findAllByStatusAndEndTimeBetween(Status.INUSE, now, endTime);

        // 1. 전송할 DTO 리스트 생성
        List<PushMessageRequest> messages = new ArrayList<>();

        for (Reservation reservation : targets) {
            Member member = reservation.getMember();
            String pushToken = member.getPushToken();

            // 토큰이 유효한 경우만 메시지 생성
            if (pushToken != null && !pushToken.isBlank()) {

                // 깔끔하게 DTO 생성 (Builder 패턴 활용)
                PushMessageRequest message = PushMessageRequest.builder()
                        .to(pushToken)
                        .title("종료 안내")
                        .body("예약하신 [" + reservation.getSpace().getName() + "] 이용 시간이 10분 남았습니다.")
                        .build();

                messages.add(message);
            }
        }

        // 2. 알림 서비스에 전송 위임 (배치 전송)
        if (!messages.isEmpty()) {
            notificationService.sendPushMessages(messages);
        }
    }

    /**
     * [스케줄러용] 이용 종료 처리 (INUSE -> COMPLETED)
     */
    @Transactional
    public void expireFinishedReservations() {
        LocalDateTime endTime = LocalDateTime.now().minusMinutes(10);

        // 1. 조건에 맞는 예약 조회 (상태: INUSE, 시간: 예약종료시간+10분 <= 현재시간)
        List<Reservation> targets = reservationRepository.findAllByStatusAndEndTimeBefore(Status.INUSE, endTime);

        // 상태 변경 INUSE -> COMPLETED
        for (Reservation reservation : targets) {
            reservation.changeStatus(Status.COMPLETED); // QR 만료됨
        }
    }
}