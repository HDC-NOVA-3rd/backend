package com.backend.nova.bill.dto;

import com.backend.nova.bill.entity.BillStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BillSearchCondition {
    private String dongNo;
    private String hoNo;
    private String billMonth;
    private BillStatus status;
    private Boolean onlyUnpaid; // true일 경우 미납(READY 또는 UNPAID)만 조회
}
