package com.backend.nova.bill.dto;

import com.backend.nova.bill.entity.BillStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class BillDetailResponse {
    private Long billId;
    private String apartmentName;
    private String dongName;
    private String hoName;
    private String billMonth;
    private String billUid;
    private BigDecimal totalPrice;
    private BillStatus status;
    private LocalDate dueDate;
    private List<BillItemResponse> items; // 상세에서만 포함
}
