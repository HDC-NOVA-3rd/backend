package com.backend.nova.reservation.entity;

public enum Status {
    CONFIRMED, // 예약 확정 (아직 입장 불가 상태)
    INUSE, // 입장 가능 (예약 시작 10분 전 - 종료 10분 후 간격, QR 이용 가능)
    CANCELLED, // 예약취소
    COMPLETED, // 예약내역 종료 (QR 만료)
}
