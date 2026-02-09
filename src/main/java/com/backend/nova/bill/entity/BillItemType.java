package com.backend.nova.bill.entity;

public enum BillItemType {
    METER,//계량기 iot센서값
    MANAGEMENT, //관리비 기본 항목
    COMMUNITY // 월,호별 커뮤니티 사용료
}
