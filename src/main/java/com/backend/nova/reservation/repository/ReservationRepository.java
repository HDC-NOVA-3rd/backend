package com.backend.nova.reservation.repository;

import com.backend.nova.reservation.entity.PaymentMethod;
import com.backend.nova.reservation.entity.Reservation;
import com.backend.nova.reservation.entity.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findAllByMemberIdOrderByStartTimeDesc(Long memberId);

    @Query("SELECT r FROM Reservation r " +
            "WHERE r.space.id = :spaceId " +
            "AND r.status != 'CANCELLED' " +
            "AND r.startTime >= :startOfDay " +
            "AND r.startTime < :endOfDay")
    List<Reservation> findAllBySpaceIdAndDate(@Param("spaceId") Long spaceId,
                                              @Param("startOfDay") LocalDateTime startOfDay,
                                              @Param("endOfDay") LocalDateTime endOfDay);

    // 중복 예약 검사 쿼리 (핵심!)
    // 조건: (기존예약시작 < 요청종료) AND (기존예약종료 > 요청시작)
    // 결과: True: 겹치는 예약이 존재함, False: 겹치는 예약이 없음
    @Query("SELECT COUNT(r) > 0 " +
            "FROM Reservation r " +
            "WHERE r.space.id = :spaceId " +
            "AND r.status != 'CANCELLED' " + // 취소된 건은 무시
            "AND r.startTime < :endTime " +
            "AND r.endTime > :startTime")
    boolean existsOverlappingReservation(@Param("spaceId") Long spaceId,
                                         @Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime);
    
    // 취소제외 커뮤니티 사용료 관리비 처리
    @Query("SELECT r FROM Reservation r " +
            "WHERE r.member.resident.ho.id = :hoId " +
            "AND r.paymentMethod = :paymentMethod " +
            "AND r.status IN :statuses " +
            "AND r.startTime BETWEEN :start AND :end")
    List<Reservation> findMonthlyManagementFeeReservations(
            @Param("hoId") Long hoId,
            @Param("paymentMethod") PaymentMethod paymentMethod,
            @Param("statuses") List<Status> statuses,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // 1. 입장 가능 상태로 변경할 예약 조회 (시작 시간 15분 전 && 상태가 CONFIRMED)
    List<Reservation> findAllByStatusAndStartTimeBefore(Status status, LocalDateTime time);

    // 2. 종료 임박 알림 보낼 예약 조회 (종료 시간 10분 전 && 상태가 IN_USE)
    List<Reservation> findAllByStatusAndEndTimeBetween(Status status, LocalDateTime start, LocalDateTime end);

    // 3. 완료 처리할 예약 조회 (종료 시간 10분 후 && 상태가 IN_USE)
    List<Reservation> findAllByStatusAndEndTimeBefore(Status status, LocalDateTime end);

    // QR 토큰으로 예약 정보 단건 조회
    Optional<Reservation> findByQrToken(String qrToken);
}