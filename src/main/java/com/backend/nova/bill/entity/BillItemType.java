package com.backend.nova.bill.entity;

public enum BillItemType {
    METER,        // 계량기 기반 요금 (전기/수도/가스)
    MANAGEMENT,  // 관리비 기본 항목
    COMMUNITY    // 커뮤니티 시설 이용료
}
