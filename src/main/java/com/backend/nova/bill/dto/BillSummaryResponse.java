package com.backend.nova.bill.dto;

import com.backend.nova.bill.entity.BillStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@AllArgsConstructor
@Builder
public class BillSummaryResponse {
    private Long billId;
    private String apartmentName;
    private String dongName;
    private String hoName;
    private String billMonth;
    private BigDecimal totalPrice;
    private BillStatus status;
    private LocalDate dueDate;
}
