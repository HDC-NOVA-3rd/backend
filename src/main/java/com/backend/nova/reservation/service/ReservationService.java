package com.backend.nova.reservation.service;

import com.backend.nova.facility.entity.Space;
import com.backend.nova.facility.repository.SpaceRepository;
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
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final SpaceRepository spaceRepository;
    private final MemberRepository memberRepository; // 회원 조회용

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

        // 5. 가격 계산 (시간 단위)
        // Duration을 사용하여 분 단위까지 정확히 계산하거나, 시간 단위로 올림 처리
        long hours = Duration.between(request.startTime(), request.endTime()).toHours();
        if (hours < 1) hours = 1; // 최소 1시간 과금
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
}