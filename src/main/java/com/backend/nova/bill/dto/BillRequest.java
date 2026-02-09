package com.backend.nova.bill.dto;

import lombok.*;

import java.time.YearMonth;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillRequest {
    private Long hoId;
    private YearMonth month;
    private Integer totalPrice;
    private List<BillItemRequest> items;
}

