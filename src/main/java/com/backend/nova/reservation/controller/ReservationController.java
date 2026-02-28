package com.backend.nova.reservation.controller;

import com.backend.nova.auth.member.MemberDetails;

import com.backend.nova.reservation.dto.OccupiedReservationResponse;

import com.backend.nova.reservation.dto.ReservationRequest;

import com.backend.nova.reservation.dto.ReservationResponse;

import com.backend.nova.reservation.service.ReservationService;

import io.swagger.v3.oas.annotations.Operation;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;

import org.springframework.http.HttpStatus;

import org.springframework.http.ResponseEntity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.web.bind.annotation.*;



import java.time.LocalDate;

import java.util.List;



@Tag(name = "예약 관리", description = "공간 예약 생성, 조회 및 취소 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reservation")
@SecurityRequirement(name = "bearerAuth")
public class ReservationController {

    private final ReservationService reservationService;

    @Operation(
            summary = "공간 예약 신청",
            description = "공간 ID, 시간, 인원 수 등을 입력받아 예약을 생성합니다. (중복 시간 발생 시 409 Conflict 반환)"
    )
    @PostMapping
    public ResponseEntity<Long> createReservation(
            @AuthenticationPrincipal MemberDetails user,
            @RequestBody ReservationRequest request
    ) {
        Long reservationId = reservationService.createReservation(user.getMemberId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(reservationId);
    }

    @Operation(summary = "내 예약 목록 조회", description = "로그인된 회원의 모든 예약 목록을 최신순으로 조회합니다.")
    @GetMapping("/me")
    public ResponseEntity<List<ReservationResponse>> getMyReservations(@AuthenticationPrincipal MemberDetails user) {
        List<ReservationResponse> myReservations = reservationService.getMyReservations(user.getMemberId());
        return ResponseEntity.ok(myReservations);
    }

    @Operation(summary = "예약 상세 조회", description = "특정 예약의 상세 정보를 조회합니다.")
    @GetMapping("/{reservationId}")
    public ResponseEntity<ReservationResponse> getReservationDetails(
            @AuthenticationPrincipal MemberDetails user,
            @PathVariable Long reservationId
    ) {
        ReservationResponse details = reservationService.getReservationDetails(user.getMemberId(), reservationId);
        return ResponseEntity.ok(details);
    }

    @Operation(summary = "예약 취소", description = "예약을 취소 상태로 변경합니다.")
    @PatchMapping("/{reservationId}/cancel")
    public ResponseEntity<Void> cancelReservation(
            @AuthenticationPrincipal MemberDetails user,
            @PathVariable Long reservationId
    ) {
        reservationService.cancelReservation(user.getMemberId(), reservationId);
        return ResponseEntity.noContent().build();
    }



    @Operation(summary = "공간별/날짜별 예약 현황(예약 불가능 시간) 조회", description = "특정 날짜에 특정 공간의 이미 예약된 시간대 목록을 조회합니다.")
    @GetMapping("/availability")
    public ResponseEntity<List<OccupiedReservationResponse>> getOccupiedReservations(
            @RequestParam Long spaceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        List<OccupiedReservationResponse> occupied = reservationService.getOccupiedReservations(spaceId, date);
        return ResponseEntity.ok(occupied);
    }

    @Operation(summary = "QR 스캔 시작", description = "Space Id 기반으로 해당 시설의 카메라 작동 시작")
    @PostMapping("/scan")
    public ResponseEntity<Void> requestQrScan(
            @AuthenticationPrincipal MemberDetails user,
            @RequestParam Long spaceId
    ) {
        // 서비스가 "검증"과 "명령"을 모두 책임짐
        reservationService.requestScan(user.getMemberId(), spaceId);
        return ResponseEntity.ok().build();
    }

}
