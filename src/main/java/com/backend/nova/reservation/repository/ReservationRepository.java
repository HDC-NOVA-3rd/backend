package com.backend.nova.reservation.repository;

import com.backend.nova.reservation.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

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
}