package com.backend.nova.bill.entity;

public enum BillStatus {
    OPEN,       // 확정 전 임시 고지서 (상시 조회)
    READY,      //발행됐지만 아직 안 냄 // 확정/발행 완료
    PAID,       // 납부 완료
    OVERDUE,    //연체
}
