package com.backend.nova.bill.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BillStatResponse {
    private long totalCount;      // 전체 건수
    private long unpaidCount;     // 미납 건수
    private long totalAmount;     // 전체 청구 금액
    private long unpaidAmount;    // 미납 총액
}
